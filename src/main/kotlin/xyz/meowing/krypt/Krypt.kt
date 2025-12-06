package xyz.meowing.krypt

import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.loader.KnitModInfo
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.krypt.api.data.StoredFile
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ServerEvent
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.managers.feature.FeatureManager
import xyz.meowing.krypt.updateChecker.UpdateChecker
import xyz.meowing.krypt.utils.modMessage

object Krypt : ClientModInitializer {
    private var showLoad = true

    const val MODRINTH_PROJECT_ID = "Orh2YGoo"

    @JvmStatic
    var LOGGER = LogManager.getLogger("krypt")

    @JvmStatic
    val NAMESPACE = "krypt"

    @JvmStatic
    val saveData = StoredFile("main/Main")

    @JvmStatic
    val modInfo = KnitModInfo("krypt", "Krypt", "1.0.2")

    var sendModLoaded: Boolean by saveData.boolean("sendModLoaded", true)

    override fun onInitializeClient() {
        ConfigManager.createConfigUI()
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()
        ConfigManager.executePending()

        EventBus.register<ServerEvent.Connect> {
            if (!showLoad) return@register

            if (sendModLoaded) {
                val loadMessage = KnitText
                    .literal("§fMod loaded.")
                    .onHover("§d${FeatureManager.moduleCount} modules §8- §d${FeatureManager.loadTime}ms §8- §d${FeatureManager.commandCount} commands")

                KnitChat.modMessage(loadMessage)
            }

            TimeScheduler.schedule(10000) {
                UpdateChecker.check()
            }

            showLoad = false
        }
    }
}