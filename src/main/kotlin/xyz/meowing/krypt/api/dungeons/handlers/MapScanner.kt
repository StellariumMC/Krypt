package xyz.meowing.krypt.api.dungeons.handlers

import net.minecraft.world.level.saveddata.maps.MapDecoration
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.DungeonAPI.rooms
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.enums.map.Checkmark
import xyz.meowing.krypt.api.dungeons.enums.map.Door
import xyz.meowing.krypt.api.dungeons.enums.map.DoorState
import xyz.meowing.krypt.api.dungeons.enums.map.DoorType
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomClearInfo
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.api.dungeons.handlers.MapUtils.mapX
import xyz.meowing.krypt.api.dungeons.handlers.MapUtils.mapZ
import xyz.meowing.krypt.api.dungeons.handlers.MapUtils.yaw
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.mixins.AccessorMapState

object MapScanner {
    private const val MAP_SIZE = 128
    private const val DOOR_CHECK_OFFSET_H1 = -128 - 4
    private const val DOOR_CHECK_OFFSET_H2 = -128 + 4
    private const val DOOR_CHECK_OFFSET_V1 = -128 * 5
    private const val DOOR_CHECK_OFFSET_V2 = 128 * 3
    private const val ROOM_COLOR_OFFSET = 5 + 128 * 4

    fun updatePlayers(state: MapItemSavedData) {
        val decorations = (state as AccessorMapState).decorations
        val players = DungeonAPI.players.filterNotNull()

        val selfDecoration = decorations.values.firstOrNull {
            it.type == MapDecorationTypes.FRAME
        }

        if (selfDecoration != null) {
            val selfPlayer = players.firstOrNull { it.name == KnitPlayer.name }
            selfPlayer?.let { updatePlayer(it, selfDecoration) }
        }

        var playerIndex = 0
        for ((_, dec) in decorations) {
            if (dec.type == MapDecorationTypes.FRAME) continue

            while (playerIndex < players.size) {
                val player = players[playerIndex++]
                if (player.name != KnitPlayer.name && !player.dead) {
                    updatePlayer(player, dec)
                    break
                }
            }
        }
    }

    private fun updatePlayer(player: DungeonPlayer, dec: MapDecoration) {
        if (player.uuid == null || player.inRender) return

        val mapSize = MapUtils.mapRoomSize.toDouble() * 6 + 20.0
        val defaultSize = ScanUtils.defaultMapSize.first.toDouble()

        player.iconX = clampMap(dec.mapX.toDouble() - MapUtils.mapCorners.first, 0.0, mapSize, 0.0, defaultSize)
        player.iconZ = clampMap(dec.mapZ.toDouble() - MapUtils.mapCorners.second, 0.0, mapSize, 0.0, defaultSize)
        player.realX = player.iconX?.let { clampMap(it, 0.0, 125.0, -200.0, -10.0) }
        player.realZ = player.iconZ?.let { clampMap(it, 0.0, 125.0, -200.0, -10.0) }
        player.yaw = dec.yaw + 180f
        player.currRoom = player.realX?.toInt()?.let { x ->
            player.realZ?.toInt()?.let { z ->
                DungeonAPI.getRoomAt(x, z)?.also { it.players.add(player) }
            }
        }
    }

    fun scan(state: MapItemSavedData) {
        val colors = state.colors
        val mapCorner = MapUtils.mapCorners
        val mapGapSize = MapUtils.mapGapSize
        val halfMapGap = mapGapSize / 2

        var cx = -1
        for (x in mapCorner.first + MapUtils.mapRoomSize / 2 until 118 step halfMapGap) {
            var cz = -1
            cx++
            for (z in mapCorner.second + MapUtils.mapRoomSize / 2 + 1 until 118 step halfMapGap) {
                cz++
                val idx = x + z * MAP_SIZE
                val center = colors.getOrNull(idx - 1) ?: continue
                val rcolor = colors.getOrNull(idx + ROOM_COLOR_OFFSET) ?: continue

                val isRoomCenter = cx % 2 == 0 && cz % 2 == 0

                if (isRoomCenter && rcolor != 0.toByte()) {
                    scanRoom(colors, cx, cz, x, z, center, rcolor, halfMapGap)
                } else if (!isRoomCenter && center != 0.toByte()) {
                    scanDoor(colors, cx, cz, idx, center)
                }
            }
        }
    }

    private fun scanRoom(colors: ByteArray, cx: Int, cz: Int, x: Int, z: Int, center: Byte, rcolor: Byte, halfMapGap: Int) {
        val rmx = cx / 2
        val rmz = cz / 2
        val roomIdx = DungeonAPI.getRoomIdx(rmx to rmz)

        val room = rooms[roomIdx] ?: Room(rmx to rmz).also {
            rooms[roomIdx] = it
            DungeonAPI.uniqueRooms.add(it)
        }

        scanRoomNeighbors(colors, cx, cz, x, z, room, halfMapGap)

        val mapRoomType = RoomType.fromMapColor(rcolor.toInt())
        if ((room.type == RoomType.UNKNOWN && room.height == null) || (room.type != mapRoomType && mapRoomType == RoomType.BLOOD)) {
            room.loadFromMapColor(rcolor)
        }

        if (rcolor == 0.toByte()) {
            room.explored = false
            return
        }

        if (center == 119.toByte() || rcolor == 85.toByte()) {
            room.explored = false
            room.checkmark = Checkmark.UNEXPLORED
            DungeonAPI.discoveredRooms["$rmx/$rmz"] = DungeonAPI.DiscoveredRoom(rmx, rmz, room)
            return
        }

        val check = when {
            rcolor == 18.toByte() && DungeonAPI.bloodKilledAll && center == 30.toByte() -> {
                if (room.checkmark != Checkmark.GREEN) roomCleared(room, Checkmark.GREEN)
                Checkmark.GREEN
            }
            rcolor == 18.toByte() && DungeonAPI.bloodSpawnedAll -> {
                if (room.checkmark != Checkmark.WHITE) roomCleared(room, Checkmark.WHITE)
                Checkmark.WHITE
            }
            center == 30.toByte() && rcolor != 30.toByte() -> {
                if (room.checkmark != Checkmark.GREEN) roomCleared(room, Checkmark.GREEN)
                Checkmark.GREEN
            }
            center == 34.toByte() -> {
                if (room.checkmark != Checkmark.WHITE) roomCleared(room, Checkmark.WHITE)
                Checkmark.WHITE
            }
            center == 18.toByte() && rcolor != 18.toByte() -> Checkmark.FAILED
            room.checkmark == Checkmark.UNEXPLORED -> {
                room.clearTime = System.currentTimeMillis()
                Checkmark.NONE
            }
            else -> null
        }

        check?.let { room.checkmark = it }
        room.explored = true
        DungeonAPI.discoveredRooms.remove("$rmx/$rmz")
    }

    private fun scanRoomNeighbors(colors: ByteArray, cx: Int, cz: Int, x: Int, z: Int, room: Room, halfMapGap: Int) {
        for ((dx, dz) in ScanUtils.mapDirections) {
            val doorCx = cx + dx
            val doorCz = cz + dz

            if (doorCx % 2 == 0 && doorCz % 2 == 0) continue

            val doorX = x + dx * halfMapGap
            val doorZ = z + dz * halfMapGap
            val doorIdx = doorX + doorZ * MAP_SIZE
            val doorCenter = colors.getOrNull(doorIdx)

            val isGap = doorCenter == null || doorCenter == 0.toByte()
            val isDoor = if (!isGap) {
                isDoorPattern(colors, doorIdx)
            } else false

            if (isGap || isDoor) continue

            val neighborCx = cx + dx * 2
            val neighborCz = cz + dz * 2
            val neighborComp = neighborCx / 2 to neighborCz / 2
            val neighborIdx = DungeonAPI.getRoomIdx(neighborComp)
            if (neighborIdx !in rooms.indices) continue

            val neighborRoom = rooms[neighborIdx]
            if (neighborRoom == null) {
                room.addComponent(neighborComp)
                rooms[neighborIdx] = room
            } else if (neighborRoom != room && neighborRoom.type != RoomType.ENTRANCE && room.type != RoomType.ENTRANCE) {
                DungeonAPI.mergeRooms(neighborRoom, room)
            }
        }
    }

    private fun scanDoor(colors: ByteArray, cx: Int, cz: Int, idx: Int, center: Byte) {
        if (!isDoorPattern(colors, idx)) return

        val comp = cx to cz
        val doorIdx = DungeonAPI.getDoorIdx(comp)
        val existingDoor = DungeonAPI.getDoorAtIdx(doorIdx)

        val rx = ScanUtils.cornerStart.first + ScanUtils.halfRoomSize + cx * ScanUtils.halfCombinedSize
        val rz = ScanUtils.cornerStart.second + ScanUtils.halfRoomSize + cz * ScanUtils.halfCombinedSize

        val type = when (center.toInt()) {
            119 -> DoorType.WITHER
            18 -> DoorType.BLOOD
            else -> DoorType.NORMAL
        }

        if (existingDoor == null) {
            val newDoor = Door(rx to rz, comp).apply {
                rotation = if (cz % 2 == 1) 0 else 1
                setType(type)
                setState(DoorState.DISCOVERED)
                updateFairyDoorStatus()
            }

            DungeonAPI.addDoor(newDoor)
        } else {
            existingDoor.setState(DoorState.DISCOVERED)

            if (existingDoor.type == DoorType.NORMAL && type != DoorType.NORMAL) {
                existingDoor.setType(type)
            }

            existingDoor.updateFairyDoorStatus()
        }
    }

    private fun isDoorPattern(colors: ByteArray, idx: Int): Boolean {
        val horiz = colors.getOrNull(idx + DOOR_CHECK_OFFSET_H1) == 0.toByte() && colors.getOrNull(idx + DOOR_CHECK_OFFSET_H2) == 0.toByte()
        val vert = colors.getOrNull(idx + DOOR_CHECK_OFFSET_V1) == 0.toByte() && colors.getOrNull(idx + DOOR_CHECK_OFFSET_V2) == 0.toByte()
        return horiz || vert
    }

    private fun roomCleared(room: Room, check: Checkmark) {
        val players = room.players
        val isGreen = check == Checkmark.GREEN
        val roomKey = room.name ?: "unknown"
        val isSolo = players.size == 1

        players.forEach { player ->
            val whiteChecks = player.getWhiteChecks()
            val greenChecks = player.getGreenChecks()
            val alreadyCleared = whiteChecks.containsKey(roomKey) || greenChecks.containsKey(roomKey)

            if (!alreadyCleared) {
                if (isSolo) player.minRooms++
                player.maxRooms++
            }

            val colorKey = if (isGreen) "GREEN" else "WHITE"
            player.clearedRooms[colorKey]?.putIfAbsent(
                roomKey,
                RoomClearInfo(
                    time = System.currentTimeMillis() - room.clearTime,
                    room = room,
                    solo = isSolo
                )
            )
        }
    }

    private fun dungeonPlayerError(decorationId: String?, reason: String?, index: Int) {
        Krypt.LOGGER.error(
            "[Dungeon Map] Dungeon player for map decoration '{}' {}. Player list index (zero-indexed): {}.",
            decorationId, reason, index
        )
    }

    private fun clampMap(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return when {
            n <= inMin -> outMin
            n >= inMax -> outMax
            else -> (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
        }
    }
}