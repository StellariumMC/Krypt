package xyz.meowing.krypt.features.alerts

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.TitleUtils.showTitle
import xyz.meowing.krypt.utils.modMessage

@Module
object UltimateAlert : Feature(
    "ultimateAlert",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val checkClass by ConfigDelegate<Boolean>("ultimateAlert.checkClass")

    private val wishNotify by ConfigDelegate<Boolean>("ultimateAlert.wishNotify")
    private val wishNotifyText by ConfigDelegate<String>("ultimateAlert.wishNotifyText")

    private val ultimateNotify by ConfigDelegate<Boolean>("ultimateAlert.ultimateNotify")
    private val ultimateNotifyText by ConfigDelegate<String>("ultimateAlert.ultimateNotifyText")

    private val castleNotify by ConfigDelegate<Boolean>("ultimateAlert.castleAlert")
    private val castleNotifyText by ConfigDelegate<String>("ultimateAlert.castleNotifyText")

    private val wishedNotify by ConfigDelegate<Boolean>("ultimateAlert.wishedNotify")
    private val wishedNotifyText by ConfigDelegate<String>("ultimateAlert.wishedNotifyText")

    private val stormEnrage by ConfigDelegate<Boolean>("ultimateAlert.stormEnrage")
    private val stormEnrageText by ConfigDelegate<String>("ultimateAlert.stormEnrageText")
    private val stormEnrageTankOnly by ConfigDelegate<Boolean>("ultimateAlert.stormEnrageTankOnly")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Ultimate alerts",
                "Shows title for when you should activate your ultimate ability.",
                "Alerts",
                ConfigElement(
                    "ultimateAlert",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Check class",
                ConfigElement(
                    "ultimateAlert.checkClass",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Wish notify",
                ConfigElement(
                    "ultimateAlert.wishNotify",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Wish notify text",
                ConfigElement(
                    "ultimateAlert.wishNotifyText",
                    ElementType.TextInput("Ultimate [Wish]")
                )
            )
            .addFeatureOption(
                "Ultimate notify",
                ConfigElement(
                    "ultimateAlert.ultimateNotify",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Ultimate notify text",
                ConfigElement(
                    "ultimateAlert.ultimateNotifyText",
                    ElementType.TextInput("Ultimate [Tank/Arch]")
                )
            )
            .addFeatureOption(
                "Castle notify",
                ConfigElement(
                    "ultimateAlert.castleAlert",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Castle notify text",
                ConfigElement(
                    "ultimateAlert.castleNotifyText",
                    ElementType.TextInput("Castle Used")
                )
            )
            .addFeatureOption(
                "Wished notify",
                ConfigElement(
                    "ultimateAlert.wishedNotify",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Wished notify text",
                ConfigElement(
                    "ultimateAlert.wishedNotifyText",
                    ElementType.TextInput("Wish Used")
                )
            )
            .addFeatureOption(
                "Storm enrage",
                ConfigElement(
                    "ultimateAlert.stormEnrage",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Storm enrage text",
                ConfigElement(
                    "ultimateAlert.stormEnrageText",
                    ElementType.TextInput("Storm Enraged!")
                )
            )
            .addFeatureOption(
                "Storm enrage tank only",
                ConfigElement(
                    "ultimateAlert.stormEnrageTankOnly",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            val message = event.message.stripped
            val playerClass = DungeonAPI.ownPlayer?.dungeonClass

            when {
                message == "⚠ Maxor is enraged! ⚠" -> {
                    if (wishNotify && (!checkClass || playerClass == DungeonClass.HEALER)) {
                        showTitle("§b${wishNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${wishNotifyText.getText()}")
                    }
                    if (ultimateNotify && (!checkClass || playerClass == DungeonClass.TANK)) {
                        showTitle("§b${ultimateNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${ultimateNotifyText.getText()}")
                    }
                }

                message == "[BOSS] Sadan: My giants! Unleashed!" -> {
                    Thread.sleep(3000)
                    if (wishNotify && (!checkClass || playerClass == DungeonClass.HEALER)) {
                        showTitle("§b${wishNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${wishNotifyText.getText()}")
                    }
                    if (ultimateNotify && (!checkClass || playerClass in listOf(DungeonClass.TANK, DungeonClass.ARCHER))) {
                        showTitle("§b${ultimateNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${ultimateNotifyText.getText()}")
                    }
                }

                message == "⚠ Storm is enraged! ⚠" -> {
                    if (stormEnrage && (!stormEnrageTankOnly || playerClass == DungeonClass.TANK)) {
                        showTitle("§b${stormEnrageText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${stormEnrageText.getText()}")
                    }
                }

                message == "[BOSS] Goldor: You have done it, you destroyed the factory…" -> {
                    if (wishNotify && (!checkClass || playerClass == DungeonClass.HEALER)) {
                        showTitle("§b${wishNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${wishNotifyText.getText()}")
                    }
                    if (ultimateNotify && (!checkClass || playerClass in listOf(DungeonClass.TANK, DungeonClass.ARCHER))) {
                        showTitle("§b${ultimateNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${ultimateNotifyText.getText()}")
                    }
                }

                message.startsWith("Your Wish healed your entire team for") && message.contains("health and shielded them for") -> {
                    if (wishedNotify) {
                        showTitle("§b${wishedNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${wishedNotifyText.getText()}")
                    }
                }

                message == "Used Castle of Stone!" -> {
                    if (castleNotify) {
                        showTitle("§b${castleNotifyText.getText()}", duration = 2000)
                        KnitChat.modMessage("§b${castleNotifyText.getText()}")
                    }
                }
            }
        }
    }

    fun String.getText(): String {
        return this.replace("&", "§")
    }
}