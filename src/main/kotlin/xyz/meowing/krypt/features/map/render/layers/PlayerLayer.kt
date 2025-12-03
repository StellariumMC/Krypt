package xyz.meowing.krypt.features.map.render.layers

import com.mojang.math.Axis
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.platform.drawTexture
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.features.map.DungeonMap
import xyz.meowing.krypt.features.map.render.MapRenderConfig
import xyz.meowing.krypt.utils.rendering.Render2D
import xyz.meowing.krypt.utils.rendering.Render2D.pushPop
import xyz.meowing.krypt.utils.rendering.Render2D.width
import java.util.UUID

//#if MC >= 1.21.8
//$$ import kotlin.math.PI
//#endif

object PlayerLayer {
    fun render(context: GuiGraphics) {
        DungeonAPI.players.filterNotNull().forEach { player ->
            if (player.dead && player.name != KnitPlayer.name) return@forEach

            val iconX = player.iconX ?: return@forEach
            val iconY = player.iconZ ?: return@forEach
            val rotation = player.yaw ?: return@forEach

            val x = iconX / 125.0 * 128.0
            val y = iconY / 125.0 * 128.0
            val isOwnPlayer = player.name == KnitPlayer.name

            if (DungeonAPI.holdingLeaps && MapRenderConfig.showPlayerNametags && (!isOwnPlayer || MapRenderConfig.showOwnPlayer)) {
                renderNametag(context, player.name, x.toFloat(), y.toFloat())
            }

            renderPlayerIcon(context, player, x, y, rotation, isOwnPlayer)
        }
    }

    private fun renderPlayerIcon(context: GuiGraphics, player: DungeonPlayer, x: Double, y: Double, rotation: Float, isOwnPlayer: Boolean) {
        context.pushPop {
            val matrix = context.pose()
            val scale = MapRenderConfig.playerIconSize.toFloat()

            //#if MC >= 1.21.8
            //$$ matrix.translate(x.toFloat(), y.toFloat())
            //$$ matrix.rotate((rotation * (PI / 180)).toFloat())
            //$$ matrix.scale(scale, scale)
            //#else
            matrix.translate(x.toFloat(), y.toFloat(), 0f)
            matrix.mulPose(Axis.ZP.rotationDegrees(rotation))
            matrix.scale(scale, scale, 1f)
            //#endif

            val showAsArrow = MapRenderConfig.showOnlyOwnHeadAsArrow && isOwnPlayer
            val showHead = MapRenderConfig.showPlayerHead && !showAsArrow

            if (showHead) renderPlayerHead(context, player) else renderPlayerArrow(context, isOwnPlayer)
        }
    }

    private fun renderPlayerHead(context: GuiGraphics, player: DungeonPlayer) {
        val borderColor = if (MapRenderConfig.iconClassColors) player.dungeonClass?.mapColor else MapRenderConfig.playerIconBorderColor

        Render2D.drawRect(context, -6, -6, 12, 12, borderColor ?: DungeonClass.defaultColor)

        val borderSize = MapRenderConfig.playerIconBorderSize.toFloat()
        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.8
            //$$ matrix.scale(1f - borderSize, 1f - borderSize)
            //#else
            matrix.scale(1f - borderSize, 1f - borderSize, 1f)
            //#endif
            Render2D.drawPlayerHead(context, -6, -6, 12, player.uuid ?: UUID(0, 0))
        }
    }

    private fun renderPlayerArrow(context: GuiGraphics, isOwnPlayer: Boolean) {
        val icon = if (isOwnPlayer) DungeonMap.markerSelf else DungeonMap.markerOther
        context.drawTexture(icon, -4, -5, 7, 10)
    }

    private fun renderNametag(context: GuiGraphics, name: String, x: Float, y: Float) {
        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.8
            //$$ matrix.translate(x, y)
            //#else
            matrix.translate(x, y, 0f)
            //#endif

            val scale = 1f / 1.3f
            val width = name.width().toFloat()
            val drawX = (-width / 2).toInt()

            //#if MC >= 1.21.8
            //$$ matrix.scale(scale, scale)
            //$$ matrix.translate(0f, 0f)
            //#else
            matrix.scale(scale, scale, 1f)
            matrix.translate(0f, 12f, 0f)
            //#endif

            val offsets = listOf(scale to 0f, -scale to 0f, 0f to scale, 0f to -scale)
            offsets.forEach { (dx, dy) ->
                context.pushPop {
                    //#if MC >= 1.21.8
                    //$$ matrix.translate(dx, dy)
                    //#else
                    matrix.translate(dx, dy, 0f)
                    //#endif
                    Render2D.renderString(context, "§0$name", drawX.toFloat(), 0f, 1f)
                }
            }

            Render2D.renderString(context, name, drawX.toFloat(), 0f, 1f)
        }
    }
}