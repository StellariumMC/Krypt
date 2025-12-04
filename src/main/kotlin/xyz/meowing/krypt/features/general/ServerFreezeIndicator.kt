package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object ServerFreezeIndicator : Feature(
    "freezeIndicator",
    "Server freeze indicator",
    "Displays when you haven't received a server tick in a certain threshold.",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Freeze Indicator"
    private val threshold by config.slider("Freeze threshold", 150.0, 2000.0, 500.0, false)
    private var lastTick = System.currentTimeMillis()

    override fun initialize() {
        HUDManager.register(NAME, "§c567ms", "freezeIndicator")

        register<GuiEvent.Render.HUD> { renderHud(it.context) }

        register<TickEvent.Server> {
            lastTick = System.currentTimeMillis()
        }
    }

    private fun renderHud(context: GuiGraphics) {
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        val now = System.currentTimeMillis()
        val timeDelta = now - lastTick

        if (timeDelta > threshold && timeDelta < 60000L /*1 minute max to make it only detect "coherent" values*/) {
            val text = "§c${timeDelta}ms"
            Render2D.renderStringWithShadow(context, text, x , y, scale)
        }
    }
}
