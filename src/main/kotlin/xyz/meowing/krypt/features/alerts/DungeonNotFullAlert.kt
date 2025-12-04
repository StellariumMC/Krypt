package xyz.meowing.krypt.features.alerts

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils.showTitle
import xyz.meowing.krypt.utils.modMessage

@Module
object DungeonNotFullAlert : Feature(
    "dungeonNotFullAlert",
    "Dungeon not full alert",
    "Shows an alert when the dungeon starts but it isn't filled",
    "Alerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            if (event.message.stripped != "Starting in 4 seconds.") return@register

            val players = DungeonAPI.players.filter { it != null }.size
            if (players == 5) return@register

            showTitle("§cNot 5/5!", duration = 2000)
            KnitChat.modMessage("§cDungeon not full §7(${players}/5)!")
        }
    }
}