package xyz.meowing.krypt.features.general

import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ItemTooltipEvent
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.Utils.toLegacyString
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.knit.api.utils.StringUtils.decodeRoman
import xyz.meowing.krypt.annotations.Command
import xyz.meowing.krypt.api.data.StoredFile
import xyz.meowing.krypt.utils.modMessage
import java.util.concurrent.ConcurrentHashMap

@Module
object BetterPartyFinder : Feature(
    "betterPartyFinder",
    "Better party finder",
    "Better party finder tooltips with player stats",
    "General",
    island = SkyBlockIsland.DUNGEON_HUB
) {
    private val showCataLevel by config.switch("Show cata level", true)
    private val showSecrets by config.switch("Show secrets", true)
    private val showSecretAverage by config.switch("Show secret average", true)
    private val showPB by config.switch("Show PB", true)
    private val showMissingClasses by config.switch("Show missing classes", true)

    private val autoKick by config.switch("Auto kick")
    private val shitterList by config.switch("Shitter list")
    private val selectedFloor by config.dropdown("Selected floor", listOf("F7", "M4", "M5", "M6", "M7"))
    private val requiredPB by config.textInput("Required PB")
    private val requiredSecrets by config.textInput("Required secrets")
    private val kickMessage by config.switch("Show kick message", true)

    private val playerCache = ConcurrentHashMap<String, PlayerData>()
    private val uuidCache = ConcurrentHashMap<String, String>()

    private val usernameRegex = Regex("§5 §\\w(\\w+)§f:")
    private val classRegex = Regex("§\\w(\\w+)§\\w \\(§e(\\d+)§b\\)")
    private val floorRegex = Regex("§7Floor: §bFloor ([IVXL]+|\\d+)")
    private val masterRegex = Regex("§7Dungeon: §bMaster Mode")
    private val partyJoinRegex = Regex("Party Finder > (\\w+) joined the dungeon group! \\((\\w+) Level (\\w+)\\)")
    private val pbRegex = Regex("^\\d+:\\d{2}$")
    private val secretsRegex = Regex("^\\d+k$")

    private val uuidApis = listOf(
        "https://playerdb.co/api/player/minecraft/",
        "https://api.mojang.com/users/profiles/minecraft/",
        "https://api.ashcon.app/mojang/v2/user/"
    )

    private val gson = Gson()

    private val kickListJson = StoredFile("features/KickList")
    var jsonObject by kickListJson.jsonObject("players")
    val kickList: MutableList<String> = mutableListOf()

    override fun initialize() {
        if (!jsonObject.has("players")) jsonObject.add("players", JsonArray())

        jsonObject.get("players").asJsonArray.forEach { element ->
            kickList.add(element.asString)
        }

        register<ItemTooltipEvent> { event ->
            val lines = event.lines
            if (lines.isEmpty()) return@register

            val strippedLines = lines.map { it.toLegacyString() }
            val (floor, isMaster) = parseFloorInfo(strippedLines) ?: return@register

            val foundClasses = mutableSetOf<String>()

            for (i in strippedLines.indices) {
                val line = strippedLines[i]
                val username = usernameRegex.find(strippedLines[i])?.groupValues?.get(1) ?: continue

                val classMatch = classRegex.find(strippedLines[i])
                val className = classMatch?.groupValues?.get(1)
                val classLevel = classMatch?.groupValues?.get(2)?.toIntOrNull()

                if (className != null) foundClasses.add(className)
                if ("§0§r§r" in line) continue

                val player = playerCache.getOrPut(username) {
                    PlayerData(username).also { fetchPlayerData(it) }
                }

                if (player.isLoading) continue

                val text = buildString {
                    append(line)
                    append("§0§r§r")

                    if (showCataLevel) {
                        if (classLevel == null) append(" §b(§e${classLevel ?: "?"}§b)§r")
                        append(" §b(§6${player.cataLevel ?: "?"}§b)§r")
                    }

                    if (showSecrets && showSecretAverage) {
                        append(" §8[§a${player.totalSecrets ?: "?"}§8/§b${player.secretAverage ?: "?"}§8]§r")
                    } else {
                        if (showSecrets) append(" §8[§a${player.totalSecrets ?: "?"}§8]§r")
                        if (showSecretAverage) append(" §8[§b${player.secretAverage ?: "?"}§8]§r")
                    }

                    if (showPB) {
                        val pb = if (isMaster) player.masterPBs[floor]?.sPlus else player.normalPBs[floor]?.sPlus
                        append(" §8[§9${pb?.let { ms ->
                            if (ms <= 0) "No S+"
                            else "${ms / 60000}:${(ms / 1000 % 60).toString().padStart(2, '0')}"
                        } ?: "?"}§8]§r")
                    }
                }

                lines[i] = Component.literal(text)
            }

            if (showMissingClasses) {
                val missing = setOf("Archer", "Berserk", "Mage", "Tank", "Healer") - foundClasses

                if (missing.isNotEmpty()) {
                    lines.add(
                        Component.literal("§cMissing:§f ${missing.joinToString(", ")}")
                    )
                }
            }
        }

        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register

            val match = partyJoinRegex.find(event.message.stripped) ?: return@register
            val username = match.groupValues[1]

            if (autoKick) {
                val player = playerCache.getOrPut(username) {
                    PlayerData(username).also { fetchPlayerData(it) }
                }

                NetworkUtils.scope.launch {
                    while (player.isLoading) delay(100)

                    checkAndKick(player)
                }
            }

            if (shitterList) {
                if (kickList.any { it.equals(username, ignoreCase = true) }) {
                    kickPlayer("kickList", username, 0, 0)
                }
            }
        }
    }

    fun addToList(name: String) {
        if (kickList.any { it.equals(name, ignoreCase = true) }) {
            KnitChat.modMessage("$name is already on the kick list!")
            return
        }

        KnitChat.modMessage("Added $name to the kick list!")
        kickList.add(name)
        save()
    }

    fun removeFromList(name: String) {
        val removed = kickList.removeIf { it.equals(name, ignoreCase = true) }

        if (removed) {
            KnitChat.modMessage("Removed $name from the kick list!")
            save()
        } else {
            KnitChat.modMessage("$name was not on the kick list.")
        }
    }

    fun removeFromList(index: Int) {
        if (index in kickList.indices) {
            val removedName = kickList.removeAt(index)
            KnitChat.modMessage("Removed $removedName from the kick list!")
            save()
        } else {
            KnitChat.modMessage("Invalid index: $index. The list has ${kickList.size} entries.")
        }
    }

    fun listEntries() {
        if (kickList.isEmpty()) {
            KnitChat.modMessage("The kick list is empty.")
            return
        }

        KnitChat.modMessage("Current entries: ")
        for(entry in kickList) {
            KnitChat.modMessage(entry)
        }
    }

    fun save() {
        val playersArray = jsonObject.getAsJsonArray("players")

        while (playersArray.size() > 0) {
            playersArray.remove(0)
        }

        kickList.forEach { entry ->
            playersArray.add(entry)
        }

        kickListJson.forceSave()
    }

    private fun parseFloorInfo(lines: List<String>): Pair<Int, Boolean>? {
        var floor: Int? = null
        var isMaster = false

        lines.forEach { line ->
            floorRegex.find(line)?.let { floor = it.groupValues[1].decodeRoman() }
            if (masterRegex.containsMatchIn(line)) isMaster = true
        }

        return floor?.let { it to isMaster }
    }

    private fun fetchPlayerData(player: PlayerData) {
        player.isLoading = true
        NetworkUtils.scope.launch {
            withContext(Dispatchers.IO) {
                val uuid = uuidCache[player.username]
                    ?: fetchUUID(player.username)?.also { uuidCache[player.username] = it }
                    ?: run {
                        player.isLoading = false
                        return@withContext
                    }

                fetchStats(uuid, player)
            }
        }
    }

    private fun fetchUUID(username: String): String? {
        repeat(uuidApis.size) { index ->
            val api = uuidApis[index]

            val connection = runCatching {
                NetworkUtils.createConnection("$api$username")
            }.getOrNull() ?: return@repeat

            val response = runCatching {
                connection.inputStream.bufferedReader().use { it.readText() }
            }.getOrNull() ?: return@repeat

            val data = runCatching {
                Gson().fromJson(response, Map::class.java) as Map<*, *>
            }.getOrNull() ?: return@repeat

            val uuid = when (index) {
                0 -> ((data["data"] as? Map<*, *>)?.get("player") as? Map<*, *>)?.get("raw_id") as? String
                1 -> data["id"] as? String
                else -> (data["uuid"] as? String)?.replace("-", "")
            }

            if (uuid != null) return uuid
        }
        return null
    }

    private fun fetchStats(uuid: String, player: PlayerData) {
        val primaryConnection = runCatching {
            NetworkUtils.createConnection("https://api.aurielyn-dev.workers.dev/dungeonstats?uuid=$uuid")
        }.getOrNull()

        if (primaryConnection != null) {
            val response = runCatching {
                primaryConnection.inputStream.bufferedReader().use { it.readText() }
            }.getOrNull()

            val data = response?.let {
                runCatching {
                    gson.fromJson(it, Map::class.java) as Map<*, *>
                }.getOrNull()
            }

            if (data != null) {
                @Suppress("UNCHECKED_CAST")
                parseStatsData(data as Map<String, Any>, player, true)
                player.isLoading = false
                return
            }
        }

        val fallbackConnection = runCatching {
            NetworkUtils.createConnection("https://sbd.evankhell.workers.dev/player/$uuid")
        }.getOrNull()

        if (fallbackConnection != null) {
            val response = runCatching {
                fallbackConnection.inputStream.bufferedReader().use { it.readText() }
            }.getOrNull()

            val data = response?.let {
                runCatching {
                    gson.fromJson(it, Map::class.java) as Map<*, *>
                }.getOrNull()
            }

            if (data != null) {
                @Suppress("UNCHECKED_CAST")
                parseStatsData(data as Map<String, Any>, player, false)
            }
        }

        player.isLoading = false
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStatsData(data: Map<String, Any>, player: PlayerData, isPrimary: Boolean) {
        val dungeons = data["dungeons"] as? Map<String, Any> ?: return

        if (isPrimary) {
            player.cataLevel = (dungeons["catacombs_experience"] as? Number)?.toDouble()?.let { getCatacombsLevel(it) }
            player.totalSecrets = (dungeons["secrets"] as? Number)?.toInt()
            player.runs = (dungeons["total_runs"] as? Number)?.toInt()
            player.secretAverage = if (player.runs != null && player.runs!! > 0 && player.totalSecrets != null) String.format("%.1f", player.totalSecrets!!.toDouble() / player.runs!!) else "0.0"

            val catacombs = dungeons["catacombs"] as? Map<String, Any>
            val masterCatacombs = dungeons["master_catacombs"] as? Map<String, Any>

            player.normalPBs = parseFloorPBs(catacombs?.get("fastest_time_s_plus") as? Map<String, Any>, true)
            player.masterPBs = parseFloorPBs(masterCatacombs?.get("fastest_time_s_plus") as? Map<String, Any>, true)
        } else {
            player.cataLevel = (dungeons["cataxp"] as? Number)?.toDouble()?.let { getCatacombsLevel(it) }
            player.totalSecrets = (dungeons["secrets"] as? Number)?.toInt()
            player.runs = (dungeons["runs"] as? Number)?.toInt()
            player.secretAverage = if (player.runs != null && player.runs!! > 0 && player.totalSecrets != null) String.format("%.1f", player.totalSecrets!!.toDouble() / player.runs!!) else "0.0"

            val pb = dungeons["pb"] as? Map<String, Any> ?: return
            player.normalPBs = parseFloorPBs(pb["catacombs"] as? Map<String, Any>, false)
            player.masterPBs = parseFloorPBs(pb["master_catacombs"] as? Map<String, Any>, false)
        }
    }

    private fun parseFloorPBs(floors: Map<String, Any>?, isPrimary: Boolean): Map<Int, FloorPB> {
        return buildMap {
            floors?.forEach { (key, value) ->
                key.toIntOrNull()?.let { floor ->
                    if (isPrimary) {
                        (value as? Number)?.toLong()?.let { time ->
                            put(floor, FloorPB(time))
                        }
                    } else {
                        (value as? Map<String, Any>)?.let { data ->
                            put(floor, FloorPB((data["rawS+"] as? Number)?.toLong()))
                        }
                    }
                }
            }
        }
    }

    private fun checkAndKick(player: PlayerData) {
        val pb = when (selectedFloor) {
            0 -> player.normalPBs[7]?.sPlus
            1 -> player.masterPBs[4]?.sPlus
            2 -> player.masterPBs[5]?.sPlus
            3 -> player.masterPBs[6]?.sPlus
            4 -> player.masterPBs[7]?.sPlus
            else -> null
        }
        val reqPB = parseRequiredPB()
        val secrets = player.totalSecrets
        val reqSecrets = parseRequiredSecrets()

        val floorName = when (selectedFloor) {
            0 -> "F7"
            1 -> "M4"
            2 -> "M5"
            3 -> "M6"
            4 -> "M7"
            else -> "?"
        }

        val pbStr = if (pb != null) formatTime(pb) else "No S+"

        KnitChat.modMessage("§f${player.username} | §6${player.cataLevel ?: "?"}§f | §a${player.totalSecrets ?: "?"}§f | §b${player.secretAverage ?: "?"}§f | §9$pbStr§f (§e$floorName§f)")

        when {
            pb != null && reqPB != null && pb > reqPB -> {
                kickPlayer("pb", player.username, pb, reqPB)
            }

            pb == null && reqPB != null && player.cataLevel != null && player.totalSecrets != null && player.runs != null -> {
                kickPlayer("pb", player.username, 0, reqPB)
            }

            secrets != null && reqSecrets != null && secrets < reqSecrets -> {
                kickPlayer("secrets", player.username, secrets.toLong(), reqSecrets)
            }
        }
    }

    private fun parseRequiredPB(): Long? {
        if (requiredPB.isEmpty()) return null

        requiredPB.toIntOrNull()?.let { return it * 1000L }

        if (pbRegex.matches(requiredPB)) {
            val parts = requiredPB.split(":")
            return (parts[0].toInt() * 60 + parts[1].toInt()) * 1000L
        }

        return null
    }

    private fun parseRequiredSecrets(): Long? {
        if (requiredSecrets.isEmpty()) return null

        requiredSecrets.toIntOrNull()?.let { return it.toLong() }

        if (secretsRegex.matches(requiredSecrets)) {
            return requiredSecrets.replace("k", "").toInt() * 1000L
        }

        return null
    }

    private fun kickPlayer(type: String, name: String, stat: Long, required: Long) {
        val privateMsg = when (type) {
            "pb" -> "§fKicking $name (PB: §e${formatTime(stat)}§f | Req: §e${formatTime(required)}§f)"
            "secrets" -> "§fKicking $name (Secrets: §e$stat§f | Req: §e$required§f)"
            "kickList" -> "§fKicking $name (on Kick List)"
            else -> "§fKicking $name"
        }

        KnitChat.modMessage(privateMsg)

        if (kickMessage) {
            val chatMsg = when (type) {
                "pb" -> "pc [Krypt] Kicking $name (PB: ${formatTime(stat)} | Req: ${formatTime(required)})"
                "secrets" -> "pc [Krypt] Kicking $name (Secrets: $stat | Req: $required)"
                "kickList" -> "pc [Krypt] Kicking $name (on Kick List)"
                else -> "pc [Krypt] Kicking $name"
            }

            TimeScheduler.schedule(400) {
                KnitChat.sendCommand(chatMsg)

                TimeScheduler.schedule(200) {
                    KnitChat.sendCommand("party kick $name")
                }
            }
        }
    }

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "No S+"
        val minutes = ms / 60000
        val seconds = (ms / 1000 % 60).toString().padStart(2, '0')
        return "$minutes:$seconds"
    }

    private fun getCatacombsLevel(xp: Double): Int {
        val reqs = listOf(50, 125, 235, 395, 625, 955, 1425, 2095, 3045, 4385, 6275, 8940, 12700, 17960, 25340,
            35640, 50040, 70040, 97640, 135640, 188140, 259640, 356640, 488640, 668640, 911640, 1239640, 1683640,
            2284640, 3084640, 4149640, 5559640, 7459640, 9959640, 13259640, 17559640, 23159640, 30359640, 39559640,
            51559640, 66559640, 85559640, 109559640, 139559640, 177559640, 225559640, 285559640, 360559640, 453559640, 569809640)

        if (xp < 0) return 0
        reqs.forEachIndexed { i, req -> if (xp < req) return i }
        return reqs.size + ((xp - reqs.last()) / 200_000_000).toInt()
    }

    private data class PlayerData(
        val username: String,
        var isLoading: Boolean = false,
        var cataLevel: Int? = null,
        var totalSecrets: Int? = null,
        var runs: Int? = null,
        var secretAverage: String? = null,
        var normalPBs: Map<Int, FloorPB> = emptyMap(),
        var masterPBs: Map<Int, FloorPB> = emptyMap()
    )

    private data class FloorPB(val sPlus: Long?)
}

@Command
object KickListCommand : Commodore("krypt") {
    init {
        literal("kick") {
            literal("add") {
                runs { name: String ->
                    BetterPartyFinder.addToList(name)
                }
            }
            literal("remove") {
                runs { name: String ->
                    BetterPartyFinder.removeFromList(name)
                }

                runs { index: Int ->
                    BetterPartyFinder.removeFromList(index)
                }
            }
            literal("list") {
                runs {
                    BetterPartyFinder.listEntries()
                }
            }
        }
    }
}