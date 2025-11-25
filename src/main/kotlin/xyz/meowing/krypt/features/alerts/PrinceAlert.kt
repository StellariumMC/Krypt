package xyz.meowing.krypt.features.alerts

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.TitleUtils

@Module
object PrinceAlert : Feature(
    "princeAlert",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Prince alert",
                "Display prince death title",
                "Alerts",
                ConfigElement(
                    "princeAlert",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Show title",
                ConfigElement(
                    "princeAlert.showTitle",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Send chat message",
                ConfigElement(
                    "princeAlert.sendMessage",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Prince message",
                ConfigElement(
                    "princeAlert.message",
                    ElementType.TextInput("Prince Killed!")
                )
            )
    }

    private val message by ConfigDelegate<String>("princeAlert.message")
    private val sendMessage by ConfigDelegate<Boolean>("princeAlert.sendMessage")
    private val showTitle by ConfigDelegate<Boolean>("princeAlert.showTitle")
    private val enabled by ConfigDelegate<Boolean>("princeAlert")

    fun displayTitle() {
        if (!enabled) return

        if (showTitle) TitleUtils.showTitle("§a$message", duration = 2000)
        if (sendMessage) KnitChat.sendMessage("/pc $message")
    }
}