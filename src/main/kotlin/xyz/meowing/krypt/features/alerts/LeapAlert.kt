package xyz.meowing.krypt.features.alerts

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature

@Module
object LeapAlert : Feature(
    "leapAlert",
    "Leap alert",
    "Announces in party chat when you use a leap",
    "Alerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val leapRegex = "^You have teleported to (.+)".toRegex()
    private val hideAfterLeap by config.switch("Hide after leap")
    private val message by config.textInput("Message",  "Leaping to {name}")
    private var hide = false

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            val result = leapRegex.find(event.message.stripped) ?: return@register
            val message = message
                .replace("{name}", result.groupValues[1])

            KnitChat.sendCommand("pc $message")
            hide = true
            TimeScheduler.schedule(3000) {
                hide = false
            }
        }

        register<RenderEvent.Entity.Pre> { event ->
            if (!hideAfterLeap) return@register
            if (!hide) return@register
            if (event.entity.id == KnitPlayer.player?.id) return@register

            event.cancel()
        }
    }
}