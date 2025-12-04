package xyz.meowing.krypt.features.alerts

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils

@Module
object MimicAlert : Feature(
    "mimicAlert",
    "Mimic alert",
    "Alerts on mimic death",
    "Alerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val showTitle by config.switch("Show title", true)
    private val sendMessage by config.switch("Send chat message", true)
    private val message by config.textInput("Mimic message", "Mimic Killed!")

    fun displayTitle() {
        if (!config()) return

        if (showTitle) TitleUtils.showTitle("§b$message", duration = 2000)
        if (sendMessage) KnitChat.sendMessage("/pc $message")
    }
}