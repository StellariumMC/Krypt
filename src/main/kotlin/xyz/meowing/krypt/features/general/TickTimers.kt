package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDEditor
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Utils.toTimerFormat
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object TickTimers : Feature(
    "tickTimers",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Tick Timers"
    private val secretTicksToggled by ConfigDelegate<Boolean>("tickTimers.secretTicks")
    private val stormTicksToggled by ConfigDelegate<Boolean>("tickTimers.stormTicks")
    private val purplePadTimerToggled by ConfigDelegate<Boolean>("tickTimers.purplePadTimer")
    private val goldorTicksToggled by ConfigDelegate<Boolean>("tickTimers.goldorTicks")

    private var secretTicks = 20
    private var stormTicks = 20
    private var goldorTicks = 60
    private var purplePadTicks = 670
    private var inStorm = false
    private var inTerms = false
    private var isStartTicks = false
    private var startTicks = 104

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Phase tick timers",
                "Shows the ticks of the current phase.",
                "General",
                ConfigElement(
                    "tickTimers",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Secret ticks",
                ConfigElement(
                    "tickTimers.secretTicks",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Storm ticks",
                ConfigElement(
                    "tickTimers.stormTicks",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Purple pad timer",
                ConfigElement(
                    "tickTimers.purplePadTimer",
                    ElementType.Switch(false)
                )
            )
            // .addFeatureOption("Goldor Ticks (Coming Soon)",
            //     ConfigElement(
            //         "tickTimers.goldorTicks",
            //         ElementType.Switch(false)
            //     )
            // )
            .addFeatureOption(
                "HudEditor",
                ConfigElement(
                    "tickTimers.hudEditor",
                    ElementType.Button("Edit Position") {
                        TickScheduler.Client.post {
                            client.execute { client.setScreen(HUDEditor()) }
                        }
                    }
                )
            )
    }

    override fun initialize() {
        HUDManager.register(NAME, "§a17", "tickTimers")

        register<GuiEvent.Render.HUD> { renderHud(it.context) }

        register<TickEvent.Server> {
            if (secretTicks > 0) secretTicks--

            if (startTicks == 0) isStartTicks = false
            if (startTicks >= 0) startTicks--

            if (goldorTicks == 0) goldorTicks = 61
            if (goldorTicks >= 0) goldorTicks--

            if (stormTicks == 0) stormTicks = 20
            if (stormTicks >= 0) stormTicks--

            if (purplePadTicks >= 0) purplePadTicks--
        }

        register<ScoreboardEvent.Update> {
            secretTicks = 20
        }

        register<ChatEvent.Receive> { event ->
            val message = event.message.stripped

            when (message) {
                "[BOSS] Storm: Pathetic Maxor, just like expected." -> {
                    inStorm = true
                    stormTicks = 20
                    purplePadTicks = 670
                }

                "[BOSS] Storm: I should have known that I stood no chance." -> {
                    inStorm = false
                    inTerms = true

                    startTicks = 104
                    isStartTicks = true
                }

                "The Core entrance is opening!" -> {
                    inTerms = false
                }
            }
        }

        register<LocationEvent.WorldChange> {
            inTerms = false
            inStorm = false
            isStartTicks = false
        }
    }

    private fun renderHud(context: GuiGraphics) {
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        if (secretTicksToggled && !DungeonAPI.inBoss) {
            val color = if (secretTicks <= 5) "§c" else if (secretTicks <= 10) "§6" else "§a"
            Render2D.renderStringWithShadow(context, "$color$secretTicks", x, y, scale)
        }

        if (stormTicksToggled && inStorm) {
            val color = if (stormTicks <= 5) "§c" else if (stormTicks <= 10) "§6" else "§a"
            Render2D.renderStringWithShadow(context, "$color$stormTicks", x, y, scale)
        }

        if (inStorm && purplePadTimerToggled && purplePadTicks > 0) {
            val color = if (purplePadTicks <= 20) "§c" else if (purplePadTicks <= 5*20) "§6" else "§a"
            val text = "§5Purple Pad §f: $color${(purplePadTicks / 20f).toTimerFormat()}"
            Render2D.renderStringWithShadow(context, text, x - (client.font.width(text) / 2f) * scale, y + 15 * scale, scale)
        }
//        if (goldorTicksToggled && inTerms) {
//            val color = if (goldorTicks <= 20) "§c" else if (goldorTicks <= 40) "§6" else "§a"
//            Render2D.renderStringWithShadow(context, if (isStartTicks) "§a$startTicks" else "$color$goldorTicks", x, y, scale)
//        }

    }
}
