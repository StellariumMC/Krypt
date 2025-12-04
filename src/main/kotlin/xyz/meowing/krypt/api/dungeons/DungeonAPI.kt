@file:Suppress("UNUSED")

package xyz.meowing.krypt.api.dungeons

import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Zombie
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.SkullBlockEntity
import tech.thatgravyboat.skyblockapi.api.data.Perk
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findOrNull
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findThenNull
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitClient.player
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonKey
import xyz.meowing.krypt.api.dungeons.enums.DungeonPhase
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
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TablistEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.alerts.MimicAlert
import xyz.meowing.krypt.features.alerts.PrinceAlert
import xyz.meowing.krypt.features.map.DungeonMap
import kotlin.collections.first
import kotlin.collections.isNotEmpty
import kotlin.math.floor

//#if MC >= 1.21.9
//$$ import tech.thatgravyboat.skyblockapi.platform.properties
//#endif

@Module
object DungeonAPI {
    private const val MIMIC_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTY3Mjc2NTM1NTU0MCwKICAicHJvZmlsZUlkIiA6ICJhNWVmNzE3YWI0MjA0MTQ4ODlhOTI5ZDA5OTA0MzcwMyIsCiAgInByb2ZpbGVOYW1lIiA6ICJXaW5zdHJlYWtlcnoiLAogICJzaWduYXR1cmVSZXF1aWJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTE5YzEyNTQzYmM3NzkyNjA1ZWY2OGUxZjg3NDlhZThmMmEzODFkOTA4NWQ0ZDRiNzgwYmExMjgyZDM1OTdhMCIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9"
    private const val RED_SKULL_TEXTURE = "eyJ0aW1lc3RhbXAiOjE1NzA5MTUxODU0ODUsInByb2ZpbGVJZCI6IjVkZTZlMTg0YWY4ZDQ5OGFiYmRlMDU1ZTUwNjUzMzE2IiwicHJvZmlsZU5hbWUiOiJBc3Nhc2luSmlhbmVyMjUiLCJzaWduYXR1cmVSZXF1aXJlZCI6dHJ1ZSwidGV4dHVyZXMiOnsiU0tJTiI6eyJ1cmwiOiJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EyMjNlMzZhYzEzZjBmNzFhYmNmYmYwYzk2ZmRjMjAxMGNjM2UxMWZmMmIwZDgxMTJkMGU2M2Y0YjRhYWEwZGUifX19"
    private const val WITHER_ESSENCE_TEXTURE = "ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ==ewogICJ0aW1lc3RhbXAiIDogMTYwMzYxMDQ0MzU4MywKICAicHJvZmlsZUlkIiA6ICIzM2ViZDMyYmIzMzk0YWQ5YWM2NzBjOTZjNTQ5YmE3ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYW5ub0JhbmFubm9YRCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lNDllYzdkODJiMTQxNWFjYWUyMDU5Zjc4Y2QxZDE3NTRiOWRlOWIxOGNhNTlmNjA5MDI0YzRhZjg0M2Q0ZDI0IgogICAgfQogIH0KfQ=="

    private val secretTypes = listOf(
        "Architect's First Draft",
        "Candycomb",
        "Decoy",
        "Defuse Kit",
        "Dungeon Chest Key",
        "Healing VIII Splash Potion",
        "Inflatable Jerry",
        "Revive Stone",
        "Secret Dye",
        "Spirit Leap",
        "Training Weights",
        "Trap",
        "Treasure Talisman"
    ).sorted()

    private val watcherSpawnedAllRegex = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    private val watcherKilledAllRegex = Regex("\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.")

    private val roomSecretsRegex = Regex("""\b([0-9]|10)/([0-9]|10)\s+Secrets\b""")
    private val dungeonFloorRegex = Regex("The Catacombs \\((?<floor>.+)\\)")

    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")

    private val witherDoorOpenRegex = Regex("\\w+ opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    private val startRegex = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.")
    private val endRegex = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")

    private val uniqueClassRegex = Regex("Your .+ stats are doubled because you are the only player using this class!")
    private val mimicRegex = Regex("""^Party > (?:\[[\w+]+] )?\w{1,16}: (.*)$""")
    private val sectionCompleteRegex = Regex("""^\w{1,16} (?:activated|completed) a \w+! \((?:7/7|8/8)\)$""")

    private val mimicMessages = listOf("mimic dead", "mimic dead!", "mimic killed", "mimic killed!", $$"$skytils-dungeon-score-mimic$")

    private val cataRegex = Regex("^ Catacombs (?<level>\\d+):")
    private val locationRegex = Regex(" *[⏣ф] *(?<location>(?:\\s?[^ൠ\\s]+)*)(?: ൠ x\\d)?")

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

    var F7Phase: DungeonPhase.F7? = null
        private set
    var P3Phase: DungeonPhase.P3? = null
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
        get() = DungeonMap.forcePaul || Perk.EZPZ.active

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

        EventBus.registerIn<ScoreboardEvent.Update>(SkyBlockIsland.THE_CATACOMBS) { event ->
            locationRegex.anyMatch(event.new, "location") { (location) ->
                dungeonFloorRegex.find(location, "floor") { (f) ->
                    val old = floor
                    val new = DungeonFloor.getByName(f)

                    if (old == new) return@find

                    floor = new
                    floor?.let { EventBus.post(DungeonEvent.Enter(it)) }
                }
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
                        PrinceAlert.displayTitle()
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
                    floorCompleted = true
                    floor?.let { EventBus.post(DungeonEvent.End(it)) }
                }

                !floorStarted && startRegex.matches(message) -> {
                    floorStarted = true
                    floor?.let { EventBus.post(DungeonEvent.Start(it)) }
                }

                sectionCompleteRegex.matches(message) -> {
                    P3Phase = P3Phase?.let {
                        DungeonPhase.P3.entries.getOrNull(it.ordinal + 1)
                    }
                }

                message == "[BOSS] Storm: I should have known that I stood no chance." -> {
                    P3Phase = DungeonPhase.P3.S1
                }

                message == "The Core entrance is opening!" -> {
                    P3Phase = null
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
                if (inBoss) return@let true
                val (x, z) = WorldScanUtils.realCoordToComponent(it.x.toInt(), it.z.toInt())
                6 * z + x > 35
            } == true

            if (floor?.floorNumber == 7 && inBoss) {
                val y = player?.y ?: return@registerIn

                F7Phase = when {
                    y > 210 -> DungeonPhase.F7.P1
                    y > 155 -> DungeonPhase.F7.P2
                    y > 100 -> DungeonPhase.F7.P3
                    y > 45 -> DungeonPhase.F7.P4
                    else -> DungeonPhase.F7.P5
                }
            }
        }

        EventBus.registerIn<EntityEvent.Death>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (mimicKilled) return@registerIn
            if (floor?.floorNumber !in listOf(6, 7)) return@registerIn
            if (inBoss) return@registerIn

            val entity = event.entity as? Zombie ?: return@registerIn
            if (!entity.isBaby) return@registerIn

            mimicKilled = true
            MimicAlert.displayTitle()
        }

        EventBus.registerIn<EntityEvent.Packet.Metadata>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (mimicKilled) return@registerIn
            if (floor?.floorNumber !in listOf(6, 7)) return@registerIn
            if (inBoss) return@registerIn

            val entity = event.entity as? Zombie ?: return@registerIn
            if (!entity.isBaby) return@registerIn
            if (entity.health > 0f) return@registerIn

            mimicKilled = true
            MimicAlert.displayTitle()
        }

        EventBus.registerIn<PacketEvent.Received>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val packet = event.packet as? ClientboundTakeItemEntityPacket ?: return@registerIn

            val itemId = packet.itemId
            val world = KnitClient.world

            val entity = world?.getEntity(itemId) as? ItemEntity ?: return@registerIn
            val name = entity.item.displayName.stripped
            val sanitizedName = name.drop(1).dropLast(1)

            if (secretTypes.binarySearch(sanitizedName) >= 0) {
                EventBus.post(DungeonEvent.Secrets.Item(itemId))
            }
        }

        EventBus.registerIn<PacketEvent.Sent>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@registerIn

            val pos = packet.hitResult.blockPos ?: return@registerIn
            val world = KnitClient.world ?: return@registerIn
            val blockState = world.getBlockState(pos)

            if (blockState.block == Blocks.CHEST || blockState.block == Blocks.TRAPPED_CHEST) {
                EventBus.post(DungeonEvent.Secrets.Chest(blockState, pos))
                return@registerIn
            }

            if (blockState.block == Blocks.LEVER) {
                EventBus.post(DungeonEvent.Secrets.Misc(DungeonEvent.Secrets.Type.LEVER, pos))
                return@registerIn
            }

            val entity = world.getBlockEntity(pos) ?: return@registerIn
            var blockTexture: String? = null

            if (entity is SkullBlockEntity) {
                val profile = entity.ownerProfile
                val texture = profile?.properties?.get("textures")

                if (texture != null && texture.isNotEmpty()) {
                    blockTexture = texture.first().value
                }
            }

            if (blockTexture == WITHER_ESSENCE_TEXTURE) {
                EventBus.post(DungeonEvent.Secrets.Essence(entity, pos))
                return@registerIn
            }

            if (blockTexture == RED_SKULL_TEXTURE) {
                EventBus.post(DungeonEvent.Secrets.Misc(DungeonEvent.Secrets.Type.RED_SKULL, pos))
                return@registerIn
            }
        }

        EventBus.registerIn<EntityEvent.Death>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (event.entity.type == EntityType.BAT) {
                EventBus.post(DungeonEvent.Secrets.Bat(event.entity))
                return@registerIn
            }
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
        inBoss = false
        F7Phase = null
        P3Phase = null
        floor = null

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

        val mimicKilledText = if (mimicKilled) "§a✔" else "§c✘"
        val princeKilledText = if (princeKilled) "§a✔" else "§c✘"

        val mimic = if (floor?.floorNumber in listOf(6, 7)) {
            "§7M: $mimicKilledText §8| §7P: $princeKilledText"
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
