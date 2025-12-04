package xyz.meowing.krypt.features.alerts

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils

@Module
object PrinceAlert : Feature(
    "princeAlert",
    "Prince alert",
    "Alerts on prince death",
    "Alerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val message by config.textInput("Message", "Prince Killed!")
    private val sendMessage by config.switch("Send chat message", true)
    private val showTitle by config.switch("Show title", true)

    fun displayTitle() {
        if (!config()) return

        if (showTitle) TitleUtils.showTitle("§a$message", duration = 2000)
        if (sendMessage) KnitChat.sendMessage("/pc $message")
    }
}