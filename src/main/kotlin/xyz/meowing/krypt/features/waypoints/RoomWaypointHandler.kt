package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.BlockPos
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomRotations

object RoomWaypointHandler {
    private val roomWaypoints = mutableMapOf<String, MutableList<Waypoint>>()

    fun loadWaypointsForRoom(room: Room) {
        val roomName = room.name ?: return
        if (roomWaypoints.containsKey(roomName)) return

        val waypointData = WaypointRegistry.getWaypointsForRoom(roomName) ?: return

        val waypoints = waypointData
            .map { it.toWaypoint(room.getRealCoord(BlockPos(it.x, it.y, it.z))) }
            .toMutableList()

        roomWaypoints[roomName] = waypoints
    }

    fun getWaypoints(room: Room): List<Waypoint>? {
        val roomName = room.name ?: return null
        return roomWaypoints[roomName]
    }

    fun clear() {
        roomWaypoints.clear()
    }

    fun reloadCurrentRoom() {
        val room = DungeonAPI.currentRoom ?: return
        val roomName = room.name ?: return

        roomWaypoints.remove(roomName)
        loadWaypointsForRoom(room)
    }

    private fun Room.getRealCoord(pos: BlockPos): BlockPos = pos.rotateAroundNorth(rotation).offset(corner?.first ?: 0, 0, corner?.third ?: 0)

    private fun BlockPos.rotateAroundNorth(rotation: RoomRotations): BlockPos =
        when (rotation) {
            RoomRotations.NORTH -> BlockPos(-this.x, this.y, -this.z)
            RoomRotations.WEST -> BlockPos(-this.z, this.y, this.x)
            RoomRotations.SOUTH -> BlockPos(this.x, this.y, this.z)
            RoomRotations.EAST -> BlockPos(this.z, this.y, -this.x)
            else -> this
        }
}