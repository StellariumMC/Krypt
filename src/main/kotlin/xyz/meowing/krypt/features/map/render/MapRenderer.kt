package xyz.meowing.krypt.features.map.render

import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.platform.drawTexture
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.features.map.DungeonMap
import xyz.meowing.krypt.features.map.render.layers.*
import xyz.meowing.krypt.features.map.render.registry.BossMapRegistry
import xyz.meowing.krypt.utils.rendering.Render2D
import xyz.meowing.krypt.utils.rendering.Render2D.pushPop
import xyz.meowing.krypt.utils.rendering.Render2D.width

object MapRenderer {
    private const val MAP_SIZE = 138

    fun render(context: GuiGraphics, x: Float, y: Float, scale: Float) {
        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.8
            //$$ matrix.translate(x, y)
            //$$ matrix.scale(scale, scale)
            //$$ matrix.translate(5f, 5f)
            //#else
            matrix.translate(x, y, 0f)
            matrix.scale(scale, scale, 1f)
            matrix.translate(5f, 5f, 0f)
            //#endif

            when {
                DungeonAPI.floorCompleted && DungeonMap.scoreMap -> renderScore(context)
                DungeonAPI.inBoss && DungeonMap.bossMap -> renderBoss(context)
                !DungeonAPI.floorCompleted && !DungeonAPI.inBoss -> renderMain(context)
            }
        }
    }

    fun renderPreview(context: GuiGraphics, x: Float, y: Float) {
        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.8
            //$$ matrix.translate(x + 5f, y + 5f)
            //#else
            matrix.translate(x + 5f, y + 5f, 0f)
            //#endif

            renderBackground(context)
            context.drawTexture(DungeonMap.defaultMap, 5, 5, 128, 128)
            if (DungeonMap.mapInfoUnder) renderPreviewInfo(context)
        }
    }

    private fun renderMain(context: GuiGraphics) {
        renderBackground(context)
        renderMainMap(context)
        if (DungeonMap.mapInfoUnder) renderInfo(context)
        if (DungeonMap.mapBorder) renderBorder(context)
    }

    private fun renderBoss(context: GuiGraphics) {
        renderBackground(context)

        val playerPos = KnitPlayer.player?.position() ?: return
        val floor = DungeonAPI.floor?.floorNumber ?: return

        val bossMap = BossMapRegistry.getBossMap(floor, playerPos) ?: return

        BossMapLayer.render(context, bossMap)
        if (DungeonMap.mapInfoUnder) renderInfo(context)
        if (DungeonMap.mapBorder) renderBorder(context)
    }

    private fun renderScore(context: GuiGraphics) {
        renderBackground(context)
        ScoreMapLayer.render(context)
        if (DungeonMap.mapInfoUnder) renderInfo(context)
        if (DungeonMap.mapBorder) renderBorder(context)
    }

    private fun renderMainMap(context: GuiGraphics) {
        val mapOffset = if (DungeonAPI.floor?.floorNumber == 1) 10.6f else 0f
        val mapScale = getScale(DungeonAPI.floor?.floorNumber)

        context.pushPop {
            translateAndScale(context, 5f, 5f, mapScale, mapOffset)

            RoomLayer.render(context)
            if (!DungeonMap.playerHeadsUnder) PlayerLayer.render(context)
            CheckmarkLayer.render(context)
            LabelLayer.render(context)
            if (DungeonMap.playerHeadsUnder) PlayerLayer.render(context)
        }
    }

    private fun renderBackground(context: GuiGraphics) {
        val height = MAP_SIZE + if (DungeonMap.mapInfoUnder) 10 else 0
        Render2D.drawRect(context, 0, 0, MAP_SIZE, height, DungeonMap.mapBackgroundColor)
    }

    private fun renderBorder(context: GuiGraphics) {
        val borderWidth = DungeonMap.mapBorderWidth
        val height = MAP_SIZE + if (DungeonMap.mapInfoUnder) 10 else 0
        val color = DungeonMap.mapBorderColor

        Render2D.drawRect(context, -borderWidth, -borderWidth, MAP_SIZE + borderWidth * 2, borderWidth, color)
        Render2D.drawRect(context, -borderWidth, height, MAP_SIZE + borderWidth * 2, borderWidth, color)
        Render2D.drawRect(context, -borderWidth, 0, borderWidth, height, color)
        Render2D.drawRect(context, MAP_SIZE, 0, borderWidth, height, color)
    }

    private fun renderInfo(context: GuiGraphics) {
        val scale = DungeonMap.mapInfoScale.toFloat()
        val line1 = DungeonAPI.mapLine1
        val line2 = DungeonAPI.mapLine2

        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.8
            //$$ matrix.translate(69f, 135f)
            //$$ matrix.scale(scale, scale)
            //#else
            matrix.translate(69f, 135f, 0f)
            matrix.scale(scale, scale, 1f)
            //#endif

            val w1 = line1.width().toFloat()
            val w2 = line2.width().toFloat()

            val style = if (DungeonMap.infoTextShadow) Render2D.TextStyle.DROP_SHADOW else Render2D.TextStyle.DEFAULT

            Render2D.renderString(context, line1, -w1 / 2f, 0f, 1f, textStyle = style)
            Render2D.renderString(context, line2, -w2 / 2f, 10f, 1f, textStyle = style)
        }
    }

    private fun renderPreviewInfo(context: GuiGraphics) {
        val scale = DungeonMap.mapInfoScale.toFloat()
        val line1 = "§7Secrets: §b?    §7Crypts: §c0    §7Mimic: §c✘"
        val line2 = "§7Min Secrets: §b?    §7Deaths: §a0    §7Score: §c0"

        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.8
            //$$ matrix.translate(69f, 135f)
            //$$ matrix.scale(scale, scale)
            //#else
            matrix.translate(69f, 135f, 0f)
            matrix.scale(scale, scale, 1f)
            //#endif

            val w1 = line1.width().toFloat()
            val w2 = line2.width().toFloat()

            val style = if (DungeonMap.infoTextShadow) Render2D.TextStyle.DROP_SHADOW else Render2D.TextStyle.DEFAULT

            Render2D.renderString(context, line1, -w1 / 2f, 0f, 1f, textStyle = style)
            Render2D.renderString(context, line2, -w2 / 2f, 10f, 1f, textStyle = style)
        }
    }

    private fun getScale(floor: Int?): Float {
        return when (floor) {
            null -> 1f
            0 -> 6f / 4f
            in 1..3 -> 6f / 5f
            else -> 1f
        }
    }

    fun translateAndScale(context: GuiGraphics, x: Float, y: Float, scale: Float, offsetX: Float = 0f, offsetY: Float = 0f) {
        val matrix = context.pose()
        //#if MC >= 1.21.8
        //$$ matrix.translate(x, y)
        //$$ if (offsetX != 0f || offsetY != 0f) matrix.translate(offsetX, offsetY)
        //$$ if (scale != 1f) matrix.scale(scale, scale)
        //#else
        matrix.translate(x, y, 0f)
        if (offsetX != 0f || offsetY != 0f) matrix.translate(offsetX, offsetY, 0f)
        if (scale != 1f) matrix.scale(scale, scale, 1f)
        //#endif
    }
}