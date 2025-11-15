@file:Suppress("UNUSED")

package xyz.meowing.krypt.api.dungeons

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.monster.Zombie
import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.utils.extentions.getTexture
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonKey
import xyz.meowing.krypt.api.dungeons.enums.map.Door
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.handlers.WorldScanner
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.handlers.DungeonPlayerManager
import xyz.meowing.krypt.api.dungeons.handlers.MapUtils
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator.deathCount
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator.foundSecrets
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator.mimicKilled
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator.princeKilled
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator.score
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator.totalSecrets
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.TablistEvent
import xyz.meowing.krypt.events.core.TickEvent
import kotlin.math.floor

@Module
object DungeonAPI {
    private const val MIMIC_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTY3Mjc2NTM1NTU0MCwKICAicHJvZmlsZUlkIiA6ICJhNWVmNzE3YWI0MjA0MTQ4ODlhOTI5ZDA5OTA0MzcwMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJXaW5zdHJlYWtlcnoiLAogICJzaWduYXR1cmVSZXF1aWJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTE5YzEyNTQzYmM3NzkyNjA1ZWY2OGUxZjg3NDlhZThmMmEzODFkOTA4NWQ0ZDRiNzgwYmExMjgyZDM1OTdhMCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"

    private val watcherSpawnedAllRegex = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    private val watcherKilledAllRegex = Regex("\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.")

    private val roomSecretsRegex = Regex("""\b([0-9]|10)/([0-9]|10)\s+Secrets\b""")
    private val dungeonFloorRegex = Regex("The Catacombs \\((?<floor>.+)\\)")

    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")

    private val witherDoorOpenRegex = Regex("\\w+ opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.")
    private val endRegex = Regex("\\s+(?:Master Mode|The) Catacombs - (?:Entrance|Floor [XVI]+)")

    private val uniqueClassRegex = Regex("Your .+ stats are doubled because you are the only player using this class!")
    private val mimicRegex = Regex("""^Party > (?:\[[\w+]+] )?\w{1,16}: (.*)$""")

    private val mimicMessages = listOf("mimic dead", "mimic dead!", "mimic killed", "mimic killed!", $$"$skytils-dungeon-score-mimic$")

    private val cataRegex = Regex("^ Catacombs (?<level>\\d+):")

    val rooms = Array<Room?>(36) { null }
    val doors = Array<Door?>(60) { null }
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>()
    val discoveredRooms = mutableMapOf<String, DiscoveredRoom>()

    var bloodOpened = false
        private set
    var bloodKilledAll = false
        private set
    var bloodSpawnedAll = false
        private set

    var floorStarted = false
        private set
    var floorCompleted = false
        private set

    var currentRoom: Room? = null
    var holdingLeaps = false
        private set

    var witherKeys = 0
        private set
    var bloodKeys = 0
        private set

    var floor: DungeonFloor? = null
        private set(value) {
            if (field != value) {
                field = value
                EventBus.post(LocationEvent.DungeonFloorChange(value))
            }
        }

    var inBoss: Boolean = false
        private set

    var mapLine1 = ""
        private set

    var mapLine2 = ""
        private set

    var uniqueClass = false
        private set

    val cryptCount: Int
        get() = ScoreCalculator.cryptsCount

    val players: Array<DungeonPlayer?>
        get() = DungeonPlayerManager.players

    val ownPlayer: DungeonPlayer?
        get() = players.find { it?.name == KnitPlayer.player?.name?.string }

    val dungeonClass: DungeonClass?
        get() = ownPlayer?.dungeonClass

    val classLevel: Int
        get() = ownPlayer?.classLevel ?: 0

    val cataLevel: Int
        get() = ownPlayer?.cataLevel ?: 0

    val isPaul: Boolean
        get() = Perk.EZPZ.active

    data class DiscoveredRoom(val x: Int, val z: Int, val room: Room)

    init {
        var tickCount = 0

        EventBus.registerIn<TablistEvent.Change>(SkyBlockIsland.DUNGEON_HUB) { event ->
            val fourthColumn = event.new.getOrNull(3) ?: return@registerIn

            fourthColumn.forEach { line ->
                cataRegex.findThenNull(line.stripped, "level") { (level) ->
                    if (level.toIntOrNull() == null || level.toIntOrNull() == cataLevel) return@findThenNull

                    ownPlayer?.cataLevel = level.toInt()
                } ?: return@registerIn
            }
        }

        EventBus.registerIn<LocationEvent.AreaChange>(SkyBlockIsland.THE_CATACOMBS) { event ->
            dungeonFloorRegex.find(event.new.name, "floor") { (f) ->
                floor = DungeonFloor.getByName(f)
                floor?.let { EventBus.post(DungeonEvent.Enter(it)) }
            }
        }

        EventBus.register<LocationEvent.IslandChange> { reset() }

        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val message = event.message.stripped

            if (event.isActionBar) {
                currentRoom?.let { room ->
                    roomSecretsRegex.findOrNull(message) { match ->
                        match[1]
                            ?.toIntOrNull()
                            ?.takeIf { it != room.secretsFound }
                            ?.let { room.secretsFound = it }
                    }
                }
                return@registerIn
            }

            if (floor?.floorNumber in listOf(6, 7)) {
                when {
                    mimicRegex.matches(message) -> {
                        if (mimicMessages.any { message.contains(it, true) }) {
                            mimicKilled = true
                            return@registerIn
                        }
                    }

                    message.equals("a prince falls. +1 bonus score", true) -> {
                        princeKilled = true
                        return@registerIn
                    }
                }
            }

            when {
                watcherSpawnedAllRegex.matches(message) -> {
                    bloodSpawnedAll = true
                }

                watcherKilledAllRegex.matches(message) -> {
                    bloodKilledAll = true
                }

                uniqueClassRegex.matches(message) -> {
                    uniqueClass = true
                }

                endRegex.matches(message) -> {
                    DungeonPlayerManager.updateAllSecrets()
                    floorCompleted = true
                    floor?.let { EventBus.post(DungeonEvent.End(it)) }
                }

                !floorStarted && startRegex.matches(message) -> {
                    floorStarted = true
                    floor?.let { EventBus.post(DungeonEvent.Start(it)) }
                }
            }

            matchWhen(message) {
                case(keyObtainedRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(keyPickedUpRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(witherDoorOpenRegex) {
                    if (witherKeys > 0) --witherKeys
                }

                case(bloodDoorOpenRegex) {
                    if (bloodKeys > 0) --bloodKeys
                    bloodOpened = true
                }
            }
        }

        EventBus.registerIn<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            updateHudLines()
            updateHeldItem()

            if (tickCount % 5 != 0) return@registerIn

            inBoss = floor != null && KnitPlayer.player?.let {
                val (x, z) = WorldScanUtils.realCoordToComponent(it.x.toInt(), it.z.toInt())
                6 * z + x > 35
            } == true
        }

        EventBus.registerIn<EntityEvent.Death>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (mimicKilled) return@registerIn
            if (floor?.floorNumber !in listOf(6, 7)) return@registerIn

            val entity = event.entity as? Zombie ?: return@registerIn
            if (!entity.isBaby) return@registerIn
            if (entity.getItemBySlot(EquipmentSlot.HEAD)?.getTexture() != MIMIC_TEXTURE) return@registerIn

            mimicKilled = true
        }
    }

    fun reset() {
        rooms.fill(null)
        doors.fill(null)
        uniqueRooms.clear()
        uniqueDoors.clear()
        discoveredRooms.clear()

        currentRoom = null
        holdingLeaps = false

        bloodKilledAll = false
        bloodSpawnedAll = false
        bloodOpened = false

        floorCompleted = false
        floorStarted = false

        mapLine1 = ""
        mapLine2 = ""

        witherKeys = 0
        bloodKeys = 0

        uniqueClass = false

        WorldScanner.reset()
        DungeonPlayerManager.reset()
        ScoreCalculator.reset()
        MapUtils.reset()
    }

    private fun handleGetKey(type: String) {
        val key = DungeonKey.getById(type) ?: return
        when (key) {
            DungeonKey.WITHER -> ++witherKeys
            DungeonKey.BLOOD -> ++bloodKeys
        }

        EventBus.post(DungeonEvent.KeyPickUp(key))
    }

    private fun updateHudLines() {
        val secrets = "§7Secrets: §b${foundSecrets}§7/§c${totalSecrets}"
        val crypts = "§7Crypts: " + when {
            cryptCount >= 5 -> "§a${cryptCount}"
            cryptCount > 0 -> "§e${cryptCount}"
            else -> "§c0"
        }

        val mimic = if (floor?.floorNumber in listOf(6, 7)) {
            "§7M: " + if (mimicKilled) "§a✔" else "§c✘" +
            "§8| §7P: " + if (princeKilled) "§a✔" else "§c✘"
        } else ""

        val unfoundSecrets = "§7Unfound: " + when {
            foundSecrets == 0 -> "§b${totalSecrets}"
            else -> "§a${totalSecrets - foundSecrets}"
        }

        val deaths = "§7Deaths: §c${deathCount.coerceAtLeast(0)}"

        val formattedScore = "§7Score: " + when {
            score >= 300 -> "§a${score}"
            score >= 270 -> "§e${score}"
            else -> "§c${score}"
        } + if (isPaul) " §b★" else ""

        mapLine1 = "$secrets $mimic $formattedScore".trim()
        mapLine2 = "$unfoundSecrets $deaths $crypts".trim()
    }

    /** Updates leap detection based on held item */
    private fun updateHeldItem() {
        val item = KnitPlayer.player?.mainHandItem ?: return
        holdingLeaps = "leap" in item.hoverName.stripped.lowercase()
    }

    // Room accessors
    fun getRoomIdx(comp: Pair<Int, Int>) = 6 * comp.second + comp.first
    fun getRoomAtIdx(idx: Int) = rooms.getOrNull(idx)
    fun getRoomAtComp(comp: Pair<Int, Int>) = getRoomAtIdx(getRoomIdx(comp))
    fun getRoomAt(x: Int, z: Int) = getRoomAtComp(WorldScanUtils.realCoordToComponent(x, z))

    fun getDoorIdx(comp: Pair<Int, Int>): Int {
        val base = ((comp.first - 1) shr 1) + 6 * comp.second
        return base - (base / 12)
    }

    fun getDoorAtIdx(idx: Int) = doors.getOrNull(idx)
    fun getDoorAtComp(comp: Pair<Int, Int>) = getDoorAtIdx(getDoorIdx(comp))
    fun getDoorAt(x: Int, z: Int) = getDoorAtComp(WorldScanUtils.realCoordToComponent(x, z))

    fun addDoor(door: Door) {
        val idx = getDoorIdx(door.componentPos)
        if (idx in doors.indices) {
            doors[idx] = door
            uniqueDoors += door
        }
    }

    fun mergeRooms(room1: Room, room2: Room) {
        uniqueRooms.remove(room2)
        for (comp in room2.components) {
            if (!room1.hasComponent(comp.first, comp.second)) {
                room1.addComponent(comp, update = false)
            }
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = room1
        }
        uniqueRooms += room1
        room1.update()
    }

    fun getMageReduction(cooldown: Double): Double {
        val multiplier = if (uniqueClass) 1 else 2
        return cooldown * (0.75 - (floor(classLevel / 2.0) / 100.0) * multiplier)
    }
}