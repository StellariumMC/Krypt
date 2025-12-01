package xyz.meowing.krypt.features.alerts

import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.TitleUtils.showTitle
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object FireFreezeAlert : Feature(
    "fireFreezeAlert",
    dungeonFloor = listOf(DungeonFloor.F3, DungeonFloor.M3)
) {
    private const val NAME = "Fire Freeze Timer"
    private var ticks = 0

    private val timer by ConfigDelegate<Boolean>("fireFreezeAlert.timer")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Fire freeze alert",
                "Shows an alert for Fire Freeze in F3/M3, along with an optional timer",
                "Alerts",
                ConfigElement(
                    "fireFreezeAlert",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Show timer",
                ConfigElement(
                    "fireFreezeAlert.timer",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        HUDManager.register(NAME, "§bFire freeze: §c4.3s", "fireFreezeTimer.timer")

        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register

            val message = event.message.stripped
            if (message == "[BOSS] The Professor: Oh? You found my Guardians' one weakness?") {
                createTimer(105,
                    onTick = {
                        if (ticks > 0) ticks--
                    },
                    onComplete = {
                        showTitle("§cFreeze!", duration = 1000)
                        ticks = 0
                    }
                )
                ticks = 100
            }
        }

        register<GuiEvent.Render.HUD> { event ->
            if (timer) renderHUD(event.context)
        }

        register<LocationEvent.WorldChange> { ticks = 0 }
    }

    private fun renderHUD(context: GuiGraphics) {
        if (ticks <= 0) return

        val text = "§bFire freeze: §c${"%.1f".format(ticks / 20.0)}s"
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        Render2D.renderString(context, text, x, y, scale)
    }
}