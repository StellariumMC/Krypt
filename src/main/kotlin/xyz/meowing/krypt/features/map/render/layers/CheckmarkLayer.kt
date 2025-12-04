package xyz.meowing.krypt.features.map.render.layers

import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.platform.drawTexture
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.map.Checkmark
import xyz.meowing.krypt.api.dungeons.enums.map.PuzzleType
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.features.map.DungeonMap
import xyz.meowing.krypt.features.map.render.MapRenderer.translateAndScale
import xyz.meowing.krypt.utils.rendering.Render2D.pushPop

object CheckmarkLayer {
    fun render(context: GuiGraphics) {
        renderDiscoveredRoomMarkers(context)
        renderRoomCheckmarks(context)
    }

    private fun renderDiscoveredRoomMarkers(context: GuiGraphics) {
        DungeonAPI.discoveredRooms.values.forEach { room ->
            val x = room.x * RoomLayer.ROOM_SPACING + RoomLayer.ROOM_RENDER_SIZE / 2
            val y = room.z * RoomLayer.ROOM_SPACING + RoomLayer.ROOM_RENDER_SIZE / 2

            context.pushPop {
                translateAndScale(context, x.toFloat(), y.toFloat(), DungeonMap.checkmarkScale.toFloat())
                context.drawTexture(Checkmark.questionMark, -5, -6, 10, 12)
            }
        }
    }

    private fun renderRoomCheckmarks(context: GuiGraphics) {
        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach

            val (centerX, centerZ) = RoomLayer.getRoomCenter(room)
            val x = (centerX * RoomLayer.ROOM_SPACING).toInt() + RoomLayer.ROOM_RENDER_SIZE / 2
            val y = (centerZ * RoomLayer.ROOM_SPACING).toInt() + RoomLayer.ROOM_RENDER_SIZE / 2
            val showCleared = DungeonMap.showClearedRoomCheckmarks && room.checkmark != Checkmark.UNEXPLORED
            val isPuzzle = room.type == RoomType.PUZZLE

            if (
                DungeonMap.renderPuzzleIcons &&
                isPuzzle
                ) {
                renderPuzzleIcon(context, room.name, x, y)
                return@forEach
            }

            val checkmark = getCheckmarkImage(room, showCleared, isPuzzle) ?: return@forEach
            val scale = if (showCleared) DungeonMap.clearedRoomCheckmarkScale.toFloat() else DungeonMap.checkmarkScale.toFloat()

            context.pushPop {
                translateAndScale(context, x.toFloat(), y.toFloat(), scale)
                context.drawTexture(checkmark, -6, -6, 12, 12)
            }
        }
    }

    private fun renderPuzzleIcon(context: GuiGraphics, puzzleName: String?, x: Int, y: Int) {
        context.pushPop {
            val scale = (DungeonMap.puzzleIconScale * DungeonMap.checkmarkScale).toFloat()
            translateAndScale(context, x.toFloat(), y.toFloat(), scale)
            val offset = if (puzzleName == "Teleport Maze") -8 else -6

            if (DungeonMap.tintPuzzleIcons) {
                val room = DungeonAPI.uniqueRooms.find { it.name == puzzleName }

                val tint = when (room?.checkmark) {
                    Checkmark.GREEN -> 0xFF00FF00.toInt()
                    Checkmark.WHITE -> 0xFFFFFF00.toInt()
                    Checkmark.FAILED -> 0xFFFF0000.toInt()
                    else -> 0xFFFFFFFF.toInt()
                }

                context.drawTexture(PuzzleType.getPuzzleIcon(puzzleName), -6, offset, 12, 12, color = tint)
            } else {
                context.drawTexture(PuzzleType.getPuzzleIcon(puzzleName), -6, offset, 12, 12)
            }
        }
    }

    private fun getCheckmarkImage(room: Room, showCleared: Boolean, isPuzzle: Boolean) = when {
        showCleared && isPuzzle -> {
            room.checkmark.image
        }

        showCleared && room.checkmark in listOf(Checkmark.GREEN, Checkmark.WHITE) -> {
            if (room.checkmark == Checkmark.GREEN) Checkmark.greenCheck else Checkmark.whiteCheck
        }

        room.type in listOf(RoomType.ENTRANCE, RoomType.PUZZLE) -> {
            null
        }

        room.type == RoomType.NORMAL && room.secrets != 0 -> {
            null
        }

        else -> room.checkmark.image
    }
}