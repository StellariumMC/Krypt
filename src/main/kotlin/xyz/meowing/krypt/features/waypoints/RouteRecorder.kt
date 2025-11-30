package xyz.meowing.krypt.features.waypoints

import com.google.gson.GsonBuilder
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.lwjgl.glfw.GLFW
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomRotations
import xyz.meowing.krypt.events.core.MouseEvent
import xyz.meowing.krypt.utils.modMessage
import java.io.File
import kotlin.math.sqrt

object RouteRecorder {
    private val waypointsFile = File("config/krypt/waypoints.json")
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    var isRecording = false
        private set

    private val currentRoute = mutableListOf<WaypointData>()
    private var currentRoomName: String? = null

    fun startRecording() {
        val roomName = DungeonAPI.currentRoom?.name
        if (roomName == null) {
            KnitChat.modMessage("§cNot in a valid dungeon room")
            return
        }

        isRecording = true
        currentRoute.clear()
        currentRoomName = roomName
        KnitChat.modMessage("§aStarted route recording for $roomName")
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false

        if (currentRoute.isEmpty()) {
            KnitChat.modMessage("§cNo waypoints recorded")
            currentRoomName = null
            return
        }

        val roomName = currentRoomName
        if (roomName == null) {
            KnitChat.modMessage("§cNo room name found")
            return
        }

        val overrideExisting = DungeonWaypoints.overrideOnSave
        saveToWaypointsFile(roomName, overrideExisting)
        KnitChat.modMessage("§a${if (overrideExisting) "Overrode" else "Added"} ${currentRoute.size} waypoints for $roomName")

        currentRoute.clear()
        currentRoomName = null
        WaypointRegistry.reloadFromLocal(notifyUser = false)
    }

    private fun saveToWaypointsFile(roomName: String, overrideExisting: Boolean) {
        waypointsFile.parentFile?.mkdirs()

        val existingData = if (waypointsFile.exists()) {
            try {
                val json = waypointsFile.readText(Charsets.UTF_8)
                gson.fromJson(json, WaypointFile::class.java)
            } catch (_: Exception) {
                WaypointFile(emptyMap())
            }
        } else {
            WaypointFile(emptyMap())
        }

        val updatedRooms = existingData.rooms.toMutableMap()

        if (overrideExisting) {
            updatedRooms[roomName] = currentRoute.toList()
        } else {
            val existingWaypoints = updatedRooms[roomName] ?: emptyList()
            updatedRooms[roomName] = existingWaypoints + currentRoute.toList()
        }

        val updatedData = WaypointFile(updatedRooms)
        waypointsFile.writeText(gson.toJson(updatedData), Charsets.UTF_8)
    }

    fun addWaypoint(type: WaypointType, blockPos: BlockPos, title: String? = null) {
        if (!isRecording) return

        val room = DungeonAPI.currentRoom ?: return
        val relativePos = room.getRelativeCoord(blockPos)

        val color = type.color
        val colorHex = String.format("#%02X%02X%02X%02X", color.red, color.green, color.blue, color.alpha)

        val aabb = getBlockAABB(blockPos)

        val waypoint = WaypointData(
            x = relativePos.x,
            y = relativePos.y,
            z = relativePos.z,
            color = colorHex,
            filled = false,
            depth = false,
            aabb = aabb,
            title = title,
            type = type.name
        )

        currentRoute.add(waypoint)
        KnitChat.modMessage("§7Added ${type.name} waypoint (${currentRoute.size} total)")
    }

    fun removeWaypoint(blockPos: BlockPos) {
        if (!isRecording) return

        val room = DungeonAPI.currentRoom ?: return
        val relativePos = room.getRelativeCoord(blockPos)

        val closestInCurrent = currentRoute.minByOrNull {
            val dx = it.x - relativePos.x
            val dy = it.y - relativePos.y
            val dz = it.z - relativePos.z
            dx * dx + dy * dy + dz * dz
        }

        if (closestInCurrent != null) {
            val distance = sqrt(((closestInCurrent.x - relativePos.x) * (closestInCurrent.x - relativePos.x) + (closestInCurrent.y - relativePos.y) * (closestInCurrent.y - relativePos.y) + (closestInCurrent.z - relativePos.z) * (closestInCurrent.z - relativePos.z)).toDouble())

            if (distance <= 2.0) {
                currentRoute.remove(closestInCurrent)
                KnitChat.modMessage("§cRemoved waypoint from current route (${currentRoute.size} remaining)")
                return
            }
        }

        val roomName = currentRoomName ?: return

        val existingData = if (waypointsFile.exists()) {
            try {
                val json = waypointsFile.readText(Charsets.UTF_8)
                gson.fromJson(json, WaypointFile::class.java)
            } catch (_: Exception) {
                return
            }
        } else {
            return
        }

        val updatedRooms = existingData.rooms.toMutableMap()
        val existingWaypoints = updatedRooms[roomName]?.toMutableList() ?: return

        val closestInFile = existingWaypoints.minByOrNull {
            val dx = it.x - relativePos.x
            val dy = it.y - relativePos.y
            val dz = it.z - relativePos.z
            dx * dx + dy * dy + dz * dz
        }

        if (closestInFile != null) {
            val distance = sqrt(((closestInFile.x - relativePos.x) * (closestInFile.x - relativePos.x) + (closestInFile.y - relativePos.y) * (closestInFile.y - relativePos.y) + (closestInFile.z - relativePos.z) * (closestInFile.z - relativePos.z)).toDouble())

            if (distance <= 2.0) {
                existingWaypoints.remove(closestInFile)
                updatedRooms[roomName] = existingWaypoints
                val updatedData = WaypointFile(updatedRooms)

                waypointsFile.writeText(gson.toJson(updatedData), Charsets.UTF_8)
                WaypointRegistry.reloadFromLocal(notifyUser = false)

                KnitChat.modMessage("§cRemoved waypoint from saved file")
            } else {
                KnitChat.modMessage("§eNo waypoint found within 2 blocks")
            }
        } else {
            KnitChat.modMessage("§eNo waypoint found at this position")
        }
    }

    private fun getBlockAABB(pos: BlockPos): AABBData {
        val world = KnitClient.client.level ?: return getDefaultAABB()
        val blockState = world.getBlockState(pos)

        val shape = blockState.getShape(world, pos)
        if (shape.isEmpty) return getDefaultAABB()
        val bounds = shape.bounds()

        return AABBData(
            minX = bounds.minX - 0.002,
            minY = bounds.minY - 0.002,
            minZ = bounds.minZ - 0.002,
            maxX = bounds.maxX + 0.002,
            maxY = bounds.maxY + 0.002,
            maxZ = bounds.maxZ + 0.002
        )
    }

    private fun getDefaultAABB() = AABBData(
        minX = -0.002,
        minY = -0.002,
        minZ = -0.002,
        maxX = 1.002,
        maxY = 1.002,
        maxZ = 1.002
    )

    fun handleRightClick(event: MouseEvent.Click) {
        if (!isRecording || event.button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return
        if (KnitClient.client.screen != null) return

        val player = KnitClient.client.player ?: return
        if (!player.isCrouching) return
        val hitResult = player.pick(5.0, 0f, false)

        if (hitResult.type == HitResult.Type.BLOCK) {
            val blockHit = hitResult as BlockHitResult
            KnitClient.client.setScreen(WaypointTypeScreen(blockHit.blockPos))
        }
    }

    fun handleLeftClick(event: MouseEvent.Click) {
        if (!isRecording || event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return
        if (KnitClient.client.screen != null) return

        val player = KnitClient.client.player ?: return
        if (!player.isCrouching) return

        val hitResult = player.pick(5.0, 0f, false)

        if (hitResult.type == HitResult.Type.BLOCK) {
            val blockHit = hitResult as BlockHitResult
            removeWaypoint(blockHit.blockPos)
            event.cancel()
        }
    }

    private fun Room.getRelativeCoord(pos: BlockPos) =
        pos.subtract(Vec3i(corner?.first ?: 0, 0, corner?.third ?: 0)).rotateToNorth(rotation)

    private fun BlockPos.rotateToNorth(rotation: RoomRotations): BlockPos =
        when (rotation) {
            RoomRotations.NORTH -> BlockPos(-this.x, this.y, -this.z)
            RoomRotations.WEST -> BlockPos(this.z, this.y, -this.x)
            RoomRotations.SOUTH -> BlockPos(this.x, this.y, this.z)
            RoomRotations.EAST -> BlockPos(-this.z, this.y, this.x)
            else -> this
        }
}