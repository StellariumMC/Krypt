package xyz.meowing.krypt.api.dungeons.handlers

import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.DungeonAPI.rooms
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.enums.map.Door
import xyz.meowing.krypt.api.dungeons.enums.map.DoorType
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomRotations
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.dungeons.utils.WorldScanUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.utils.WorldUtils

@Module
object WorldScanner {
    private const val SCAN_INTERVAL = 5
    private const val DOOR_HEIGHT_THRESHOLD = 85
    private const val DUNGEON_MIN = -200.0
    private const val DUNGEON_MAX = -10.0

    val availableComponents = ScanUtils.getScanCoord().toMutableList()
    var lastIdx: Int? = null
    private var tickCounter = 0

    init {
        EventBus.registerIn<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            val player = KnitClient.player ?: return@registerIn
            checkPlayerState()

            val (x, z) = WorldScanUtils.realCoordToComponent(player.x.toInt(), player.z.toInt())
            val idx = 6 * z + x
            if (idx > 35) return@registerIn

            if (++tickCounter % SCAN_INTERVAL == 0) {
                scan()
                checkRoomState()
                checkDoorState()
            }

            val prevRoom = lastIdx?.let { rooms[it] }
            val currRoom = rooms.getOrNull(idx)

            if (prevRoom != null && currRoom != null && prevRoom != currRoom) {
                EventBus.post(DungeonEvent.Room.Change(prevRoom, currRoom))
            }

            if (lastIdx != idx || (currRoom != null && DungeonAPI.currentRoom == null)) {
                lastIdx = idx
                DungeonAPI.currentRoom = currRoom
                currRoom?.let { room ->
                    room.explored = true
                    room.components.firstOrNull()?.let { (rmx, rmz) ->
                        DungeonAPI.discoveredRooms.remove("$rmx/$rmz")
                    }
                }
            }
        }
    }

    fun reset() {
        availableComponents.clear()
        availableComponents += ScanUtils.getScanCoord()
        lastIdx = null
        tickCounter = 0
    }

    fun scan() {
        if (availableComponents.isEmpty()) return

        val world = KnitClient.world ?: return

        for (idx in availableComponents.indices.reversed()) {
            val (cx, cz, rxz) = availableComponents[idx]
            val (rx, rz) = rxz

            if (!WorldScanUtils.isChunkLoaded(rx, rz)) continue
            if (world.getChunk(rx shr 4, rz shr 4).javaClass.simpleName == "FakeChunk") continue

            val height = WorldScanUtils.getHighestY(rx, rz) ?: continue
            availableComponents.removeAt(idx)

            if (cx % 2 == 1 || cz % 2 == 1) {
                scanDoor(cx, cz, rx, rz, height)
                continue
            }

            scanRoom(cx, cz, rx, rz, height)
        }
    }

    private fun scanDoor(cx: Int, cz: Int, rx: Int, rz: Int, height: Int) {
        if (height >= DOOR_HEIGHT_THRESHOLD) return
        val comp = cx to cz
        val doorIdx = DungeonAPI.getDoorIdx(comp)
        if (DungeonAPI.getDoorAtIdx(doorIdx) != null) return

        val door = Door(rx to rz, comp).apply {
            rotation = if (cz % 2 == 1) 0 else 1
            type = when (WorldUtils.getBlockNumericId(rx, 69, rz)) {
                173 -> DoorType.WITHER
                97 -> DoorType.ENTRANCE
                159 -> DoorType.BLOOD
                else -> DoorType.NORMAL
            }
        }
        DungeonAPI.addDoor(door)
    }

    private fun scanRoom(cx: Int, cz: Int, rx: Int, rz: Int, height: Int) {
        val x = cx / 2
        val z = cz / 2
        val roomIdx = DungeonAPI.getRoomIdx(x to z)

        val room = rooms[roomIdx]?.apply {
            if (this.height == null) this.height = height
            scan()
        } ?: Room(x to z, height).scan().also {
            rooms[roomIdx] = it
            DungeonAPI.uniqueRooms.add(it)
        }

        scanNeighbors(room, cx, cz, x, z, rx, rz, height)
    }

    private fun scanNeighbors(room: Room, cx: Int, cz: Int, x: Int, z: Int, rx: Int, rz: Int, height: Int) {
        for ((dx, dz, cxOff, zOff) in ScanUtils.directions) {
            val nx = rx + dx
            val nz = rz + dz
            val blockBelow = WorldUtils.getBlockNumericId(nx, height, nz)
            val blockAbove = WorldUtils.getBlockNumericId(nx, height + 1, nz)

            if (room.type == RoomType.ENTRANCE && blockBelow != 0) continue
            if (blockBelow == 0 || blockAbove != 0) continue

            val neighborComp = (x + cxOff) to (z + zOff)
            val neighborIdx = DungeonAPI.getRoomIdx(neighborComp)
            if (neighborIdx !in rooms.indices) continue

            val neighborRoom = rooms[neighborIdx]
            when {
                neighborRoom == null -> {
                    room.addComponent(neighborComp)
                    rooms[neighborIdx] = room
                }

                neighborRoom != room && neighborRoom.type != RoomType.ENTRANCE && room.type != RoomType.ENTRANCE -> {
                    val doorComp = (cx + dx) to (cz + dz)
                    val doorIdx = DungeonAPI.getDoorIdx(doorComp)

                    if (doorIdx !in DungeonAPI.doors.indices || DungeonAPI.doors[doorIdx] == null) {
                        DungeonAPI.mergeRooms(neighborRoom, room)
                    }
                }
            }
        }
    }

    fun checkPlayerState() {
        val world = KnitClient.world ?: return
        val networkHandler = KnitClient.client.connection

        for (player in DungeonAPI.players.filterNotNull()) {
            val entity = world.players().find { it.name.string == player.name }
            val ping = networkHandler?.getPlayerInfo(entity?.uuid)?.latency ?: -1

            player.inRender = ping != -1 && entity != null

            if (player.inRender && entity != null) {
                onPlayerMove(player, entity.x, entity.z, entity.yRot)
            }

            if (ping == -1) continue

            val currRoom = player.currRoom ?: continue
            if (currRoom != player.lastRoom) {
                player.lastRoom?.players?.remove(player)
                currRoom.players.add(player)
                player.lastRoom = currRoom
            }
        }
    }

    fun checkRoomState() {
        for (room in rooms.filterNotNull()) {
            if (room.rotation == RoomRotations.NONE) {
                room.findRotation()
            }

            if (room.componentCenters.size < room.components.size) {
                room.findComponentCenters()
                room.findCenter()
            }
        }
    }

    fun checkDoorState() {
        for (door in DungeonAPI.uniqueDoors) {
            if (!door.opened) {
                door.check()
            }
        }
    }

    private fun onPlayerMove(player: DungeonPlayer, x: Double, z: Double, yaw: Float) {
        player.realX = x
        player.realZ = z
        player.yaw = yaw + 180f

        if (x !in DUNGEON_MIN..DUNGEON_MAX || z !in DUNGEON_MIN..DUNGEON_MAX) return

        val mapSize = ScanUtils.defaultMapSize
        player.iconX = clampMap(x, DUNGEON_MIN, DUNGEON_MAX, 0.0, mapSize.first.toDouble())
        player.iconZ = clampMap(z, DUNGEON_MIN, DUNGEON_MAX, 0.0, mapSize.second.toDouble())
        player.currRoom = DungeonAPI.getRoomAt(x.toInt(), z.toInt())
    }

    private fun clampMap(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return when {
            n <= inMin -> outMin
            n >= inMax -> outMax
            else -> (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
        }
    }
}