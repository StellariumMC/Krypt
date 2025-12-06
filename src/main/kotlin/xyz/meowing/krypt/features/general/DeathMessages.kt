package xyz.meowing.krypt.features.general

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.features.Feature

@Module
object DeathMessages : Feature(
    "deathMessages",
    "Death messages",
    "Sends messages on player deaths",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val message by config.textInput("Message", "{name} died! {count} deaths yet.")

    override fun initialize() {
        register<DungeonEvent.Player.Death> { event ->
            val message = message
                .replace("{name}", event.player.name)
                .replace("{count}", event.player.deaths.toString())

            KnitChat.sendCommand("pc $message")
        }
    }
}