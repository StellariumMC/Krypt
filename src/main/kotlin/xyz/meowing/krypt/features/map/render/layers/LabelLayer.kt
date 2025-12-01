package xyz.meowing.krypt.features.map.render.layers

import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.map.Checkmark
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomShape
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.features.map.render.MapRenderConfig
import xyz.meowing.krypt.utils.rendering.Render2D
import xyz.meowing.krypt.utils.rendering.Render2D.pushPop
import xyz.meowing.krypt.utils.rendering.Render2D.width

object LabelLayer {
    fun render(context: GuiGraphics) {
        DungeonAPI.uniqueRooms.forEach { room ->
            if (!room.explored) return@forEach

            val checkmarkMode = when (room.type) {
                RoomType.PUZZLE -> MapRenderConfig.puzzleCheckmarkMode
                RoomType.NORMAL, RoomType.YELLOW, RoomType.TRAP, RoomType.UNKNOWN -> MapRenderConfig.normalCheckmarkMode
                else -> return@forEach
            }

            if (checkmarkMode < 1) return@forEach

            renderRoomLabel(context, room, checkmarkMode)
        }
    }

    private fun renderRoomLabel(context: GuiGraphics, room: Room, checkmarkMode: Int) {
        val secrets = if (room.checkmark == Checkmark.GREEN) room.secrets else room.secretsFound

        val roomNameColor = when (room.checkmark) {
            Checkmark.FAILED -> MapRenderConfig.roomTextFailedColor.code
            Checkmark.GREEN -> MapRenderConfig.roomTextSecretsColor.code
            Checkmark.WHITE -> MapRenderConfig.roomTextClearedColor.code
            else -> MapRenderConfig.roomTextNotClearedColor.code
        }

        val secretsColor = when (room.checkmark) {
            Checkmark.GREEN -> MapRenderConfig.secretsTextSecretsColor.code
            Checkmark.WHITE -> MapRenderConfig.secretsTextClearedColor.code
            else -> MapRenderConfig.secretsTextNotClearedColor.code
        }

        val roomText = room.name ?: "???"
        val secretText = "$secrets/${room.secrets}"

        val lines = buildList {
            if (checkmarkMode in listOf(1, 3)) {
                addAll(roomText.split(" ").map { roomNameColor + it })
            }

            if (checkmarkMode in listOf(2, 3) && room.secrets != 0) {
                add(secretsColor + secretText)
            }
        }

        val (centerX, centerZ) = RoomLayer.getRoomCenter(room)
        val baseScale = (0.75f * MapRenderConfig.roomLabelScale).toFloat()
        val scale = if (MapRenderConfig.scaleTextToFitRoom) calculateFittedScale(lines, baseScale, room) else baseScale

        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.8
            //$$ matrix.translate((centerX * RoomLayer.ROOM_SPACING).toFloat() + RoomLayer.ROOM_RENDER_SIZE / 2, (centerZ * RoomLayer.ROOM_SPACING).toFloat() + RoomLayer.ROOM_RENDER_SIZE / 2)
            //$$ matrix.scale(scale, scale)
            //#else
            matrix.translate((centerX * RoomLayer.ROOM_SPACING).toFloat() + RoomLayer.ROOM_RENDER_SIZE / 2, (centerZ * RoomLayer.ROOM_SPACING).toFloat() + RoomLayer.ROOM_RENDER_SIZE / 2, 0f)
            matrix.scale(scale, scale, 1f)
            //#endif

            lines.forEachIndexed { i, line ->
                val drawX = (-line.width() / 2).toFloat()
                val drawY = (9 * i - (lines.size * 9) / 2).toFloat()

                if (MapRenderConfig.textShadow) {
                    renderTextShadow(context, line.stripColor(), drawX.toInt(), drawY.toInt(), scale)
                }

                Render2D.renderString(context, line, drawX, drawY, 1f)
            }
        }
    }

    private fun calculateFittedScale(lines: List<String>, baseScale: Float, room: Room): Float {
        val visualWidth = lines.maxOfOrNull { it.stripColor().width() * baseScale } ?: return baseScale

        val roomWidth = when (room.shape) {
            RoomShape.SHAPE_1X1 -> RoomLayer.ROOM_RENDER_SIZE
            RoomShape.SHAPE_1X2 -> {
                val hasHorizontalLayout = room.components.map { it.first }.toSet().size > 1
                if (hasHorizontalLayout) RoomLayer.ROOM_RENDER_SIZE * 2 + RoomLayer.ROOM_SPACING - RoomLayer.ROOM_RENDER_SIZE
                else RoomLayer.ROOM_RENDER_SIZE
            }
            RoomShape.SHAPE_1X3 -> {
                val hasHorizontalLayout = room.components.map { it.first }.toSet().size > 1
                if (hasHorizontalLayout) RoomLayer.ROOM_RENDER_SIZE * 3 + (RoomLayer.ROOM_SPACING - RoomLayer.ROOM_RENDER_SIZE) * 2
                else RoomLayer.ROOM_RENDER_SIZE
            }
            RoomShape.SHAPE_1X4 -> {
                val hasHorizontalLayout = room.components.map { it.first }.toSet().size > 1
                if (hasHorizontalLayout) RoomLayer.ROOM_RENDER_SIZE * 4 + (RoomLayer.ROOM_SPACING - RoomLayer.ROOM_RENDER_SIZE) * 3
                else RoomLayer.ROOM_RENDER_SIZE
            }
            RoomShape.SHAPE_2X2 -> RoomLayer.ROOM_RENDER_SIZE * 2 + RoomLayer.ROOM_SPACING - RoomLayer.ROOM_RENDER_SIZE
            RoomShape.SHAPE_L -> RoomLayer.ROOM_RENDER_SIZE * 2 + RoomLayer.ROOM_SPACING - RoomLayer.ROOM_RENDER_SIZE
            else -> RoomLayer.ROOM_RENDER_SIZE
        }

        val maxWidth = (roomWidth - 4).toFloat()
        val scaleX = if (visualWidth > maxWidth) maxWidth / visualWidth else 1f

        return baseScale * scaleX
    }

    private fun renderTextShadow(context: GuiGraphics, text: String, x: Int, y: Int, scale: Float) {
        val offsets = listOf(scale to 0f, -scale to 0f, 0f to scale, 0f to -scale)

        offsets.forEach { (dx, dy) ->
            context.pushPop {
                val matrix = context.pose()

                //#if MC >= 1.21.8
                //$$ matrix.translate(dx, dy)
                //#else
                matrix.translate(dx, dy, 0f)
                //#endif

                Render2D.renderString(context, "§0$text", x.toFloat(), y.toFloat(), 1f)
            }
        }
    }
}