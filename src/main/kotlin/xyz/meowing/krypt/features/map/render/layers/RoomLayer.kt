package xyz.meowing.krypt.features.map.render.layers

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.map.*
import xyz.meowing.krypt.features.map.DungeonMap
import xyz.meowing.krypt.utils.rendering.Render2D
import java.awt.Color

object RoomLayer {
    private const val ROOM_SIZE = 18
    private const val GAP_SIZE = 4
    private const val SPACING = ROOM_SIZE + GAP_SIZE

    const val ROOM_SPACING = SPACING
    const val ROOM_RENDER_SIZE = ROOM_SIZE

    fun render(context: GuiGraphics) {
        renderDiscoveredRooms(context)
        renderExploredRooms(context)
        renderDoors(context)
    }

    private fun renderDiscoveredRooms(context: GuiGraphics) {
        DungeonAPI.discoveredRooms.values.forEach { room ->
            Render2D.drawRect(
                context,
                room.x * SPACING,
                room.z * SPACING,
                ROOM_SIZE,
                ROOM_SIZE,
                Color(65, 65, 65, 255)
            )
        }
    }

    private fun renderExploredRooms(context: GuiGraphics) {
        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach

            room.components.forEach { (x, z) ->
                Render2D.drawRect(
                    context,
                    x * SPACING,
                    z * SPACING,
                    ROOM_SIZE,
                    ROOM_SIZE,
                    room.type.color
                )
            }

            renderRoomConnectors(context, room)
        }
    }

    private fun renderDoors(context: GuiGraphics) {
        DungeonAPI.uniqueDoors.forEach { door ->
            if (door.state != DoorState.DISCOVERED) return@forEach

            val type =
                if (door.opened && !door.isFairyDoor && door.type == DoorType.WITHER && DungeonMap.changeDoorColorOnOpen) DoorType.NORMAL else door.type

            val (cx, cy) = door.componentPos.let { it.first / 2 * SPACING to it.second / 2 * SPACING }
            val isVertical = door.rotation == 0
            val (width, height) = if (isVertical) 6 to 4 else 4 to 6
            val (x, y) = if (isVertical) cx + 6 to cy + 18 else cx + 18 to cy + 6

            Render2D.drawRect(context, x, y, width, height, type.color)
        }
    }

    private fun renderRoomConnectors(context: GuiGraphics, room: Room) {
        val directions = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

        room.components.forEach { (x, z) ->
            directions.forEach { (dx, dz) ->
                val nx = x + dx
                val nz = z + dz
                if (!room.hasComponent(nx, nz)) return@forEach

                val cx = (x + nx) / 2 * SPACING
                val cy = (z + nz) / 2 * SPACING
                val isVertical = dx == 0
                val width = if (isVertical) ROOM_SIZE else GAP_SIZE
                val height = if (isVertical) GAP_SIZE else ROOM_SIZE
                val drawX = if (isVertical) cx else cx + ROOM_SIZE
                val drawY = if (isVertical) cy + ROOM_SIZE else cy

                Render2D.drawRect(context, drawX, drawY, width, height, room.type.color)
            }
        }

        if (room.components.size == 4 && room.shape == RoomShape.SHAPE_2X2) {
            val x = room.components[0].first * SPACING + ROOM_SIZE
            val y = room.components[0].second * SPACING + ROOM_SIZE
            Render2D.drawRect(context, x, y, GAP_SIZE, GAP_SIZE, room.type.color)
        }
    }

    fun getRoomCenter(room: Room): Pair<Double, Double> {
        val minX = room.components.minOf { it.first }
        val minZ = room.components.minOf { it.second }
        val maxX = room.components.maxOf { it.first }
        val maxZ = room.components.maxOf { it.second }

        val width = maxX - minX
        val height = maxZ - minZ

        var centerZ = minZ + height / 2.0
        if (room.shape == RoomShape.SHAPE_L) {
            val topEdgeCount = room.components.count { it.second == minZ }
            centerZ += if (topEdgeCount == 2) -height / 2.0 else height / 2.0
        }

        return minX + width / 2.0 to centerZ
    }
}