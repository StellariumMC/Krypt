package xyz.meowing.krypt.features.map.render.layers

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.state.MapRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.MapItem
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.utils.rendering.Render2D.pushPop

object ScoreMapLayer {
    private var cachedRenderState = MapRenderState()

    fun render(context: GuiGraphics) {
        val renderState = getCurrentRenderState() ?: return

        context.pushPop {
            val matrix = context.pose()
            //#if MC >= 1.21.7
            //$$ matrix.translate(5f, 5f)
            //$$ context.submitMapRenderState(renderState)
            //#else
            val consumer = KnitClient.client.renderBuffers().bufferSource()
            matrix.translate(5f, 5f, 5f)
            KnitClient.client.mapRenderer.render(renderState, matrix, consumer, true, LightTexture.FULL_BRIGHT)
            //#endif
        }
    }

    private fun getCurrentRenderState(): MapRenderState? {
        val cache = cachedRenderState.takeIf { it.texture != null }

        if (KnitPlayer.player == null || KnitClient.world == null) return cache

        val stack = KnitPlayer.player?.inventory?.getItem(8).takeIf { it?.item is MapItem } ?: return cache
        val mapId = stack.get(DataComponents.MAP_ID) ?: return cache
        val mapData = MapItem.getSavedData(mapId, KnitClient.world!!) ?: return cache

        val renderState = MapRenderState()
        KnitClient.client.mapRenderer.extractRenderState(mapId, mapData, renderState)
        
        cachedRenderState = renderState
        return renderState
    }
}