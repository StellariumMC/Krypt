package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.ScoreboardEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.Utils.toTimerFormat
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object TickTimers : Feature(
    "tickTimers",
    "Phase tick timers",
    "Shows the ticks of the current phase.",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Tick Timers"
    private val secretTicks by config.switch("Secret ticks")
    private val stormTicks by config.switch("Storm ticks")
    private val goldorTicks by config.switch("Goldor ticks")
    private val purplePadTimer by config.switch("Purple pad timer")

    private var _secretTicks = 20
    private var _stormTicks = 20
    private var _goldorTicks = 60
    private var _purplePadTicks = 670

    private var inStorm = false
    private var inGoldor = false
    private var inTerms = false
    private var isStartTicks = false
    private var startTicks = 104

    override fun initialize() {
        HUDManager.register(NAME, "§a17", "tickTimers")

        register<GuiEvent.Render.HUD> { renderHud(it.context) }

        register<TickEvent.Server> {
            if (_secretTicks > 0) _secretTicks--

            if (startTicks == 0) isStartTicks = false
            if (startTicks >= 0) startTicks--

            if (_goldorTicks == 0) _goldorTicks = 61
            if (_goldorTicks >= 0) _goldorTicks--

            if (_stormTicks == 0) _stormTicks = 20
            if (_stormTicks >= 0) _stormTicks--

            if (_purplePadTicks >= 0) _purplePadTicks--
        }

        register<ScoreboardEvent.Update> {
            _secretTicks = 20
        }

        register<ChatEvent.Receive> { event ->
            val message = event.message.stripped

            when (message) {
                "[BOSS] Storm: Pathetic Maxor, just like expected." -> {
                    inStorm = true
                    _stormTicks = 20
                    _purplePadTicks = 670
                }

                "[BOSS] Storm: I should have known that I stood no chance." -> {
                    inStorm = false
                    inTerms = true

                    startTicks = 104
                    isStartTicks = true
                }

                "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                    inGoldor = true
                    _goldorTicks = 60
                }

                "The Core entrance is opening!" -> {
                    inTerms = false
                    inGoldor = false
                }
            }
        }

        register<LocationEvent.WorldChange> {
            inTerms = false
            inStorm = false
            inGoldor = false
            isStartTicks = false
        }
    }

    private fun renderHud(context: GuiGraphics) {
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        if (secretTicks && !DungeonAPI.inBoss) {
            val color = if (_secretTicks <= 5) "§c" else if (_secretTicks <= 10) "§6" else "§a"
            Render2D.renderStringWithShadow(context, "$color$_secretTicks", x, y, scale)
        }

        if (stormTicks && inStorm) {
            val color = if (_stormTicks <= 5) "§c" else if (_stormTicks <= 10) "§6" else "§a"
            Render2D.renderStringWithShadow(context, "$color$_stormTicks", x, y, scale)
        }

        if (inStorm && purplePadTimer && _purplePadTicks > 0) {
            val color = if (_purplePadTicks <= 20) "§c" else if (_purplePadTicks <= 5*20) "§6" else "§a"
            val text = "§5Purple Pad §f: $color${(_purplePadTicks / 20f).toTimerFormat()}"
            Render2D.renderStringWithShadow(context, text, x - (client.font.width(text) / 2f) * scale, y + 15 * scale, scale)
        }

        if (goldorTicks && inGoldor) {
            val seconds = _goldorTicks / 20
            val color = if (seconds <= 1.0) "§c" else if (seconds <= 2.0) "§6" else "§a"
            Render2D.renderStringWithShadow(context, "$color%.2f".format(seconds), x, y, scale)
        }
    }
}
