package xyz.meowing.krypt.features.general

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.LocationAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils.showTitle

@Module
object CryptReminder : Feature(
    "cryptReminder",
    "Crypt reminder",
    "Shows reminder about the crypt count if its below 5",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val delay by config.slider("Delay", 2.0, 1.0, 5.0, false)
    private val sendToParty by config.switch("Send to party", true)

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            if (event.message.stripped != "[NPC] Mort: Good luck.") return@register

            TimeScheduler.schedule(1000 * 60 * delay.toLong()) {
                val cryptCount = DungeonAPI.cryptCount

                if (cryptCount >= 5) return@schedule
                if (LocationAPI.island != SkyBlockIsland.THE_CATACOMBS) return@schedule
                if (DungeonAPI.inBoss) return@schedule

                showTitle("§c$cryptCount§7/§c5 §fcrypts", null, 3000)
                if (sendToParty) KnitChat.sendCommand("pc $cryptCount/5 crypts")
            }
        }
    }
}