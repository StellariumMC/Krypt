package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDEditor
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object ServerFreezeIndicator : Feature(
    "freezeIndicator",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Freeze Indicator"
    private val threshold by ConfigDelegate<Double>("freezeIndicator.threshold")
    private var lastTick = System.currentTimeMillis()

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Server freeze indicator",
                "Displays when you haven't received a server tick in a certain threshold.",
                "General",
                ConfigElement(
                    "freezeIndicator",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Freeze threshold",
                ConfigElement(
                    "freezeIndicator.threshold",
                    ElementType.Slider(150.0, 2000.0, 500.0, false)
                )
            )
            .addFeatureOption(
                "HudEditor",
                ConfigElement(
                    "freezeIndicator.hudEditor",
                    ElementType.Button("Edit Position") {
                        TickScheduler.Client.post {
                            client.execute { client.setScreen(HUDEditor()) }
                        }
                    }
                )
            )
    }

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
