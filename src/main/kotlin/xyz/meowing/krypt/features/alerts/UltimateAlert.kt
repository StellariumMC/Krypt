package xyz.meowing.krypt.features.alerts

import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils.showTitle
import xyz.meowing.krypt.utils.modMessage

@Module
object UltimateAlert : Feature(
    "ultimateAlert",
    "Ultimate alerts",
    "Shows title for when you should activate your ultimate ability.",
    "Alerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val checkClass by config.switch("Check class", true)

    private val wishNotify by config.switch("Wish notify", true)
    private val wishNotifyText by config.textInput("Wish notify text", "Ultimate [Wish]")

    private val ultimateNotify by config.switch("Ultimate notify", true)
    private val ultimateNotifyText by config.textInput("Ultimate notify text", "Ultimate [Tank/Arch]")

    private val castleNotify by config.switch("Castle notify", true)
    private val castleNotifyText by config.textInput("Castle notify text", "Castle Used")

    private val wishedNotify by config.switch("Wished notify", true)
    private val wishedNotifyText by config.textInput("Wished notify text", "Wish Used")

    private val stormEnrage by config.switch("Storm enrage", true)
    private val stormEnrageText by config.textInput("Storm enrage text", "Storm Enraged!")
    private val stormEnrageTankOnly by config.switch("Storm enrage tank only")

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
                    TimeScheduler.schedule(3000) {
                        if (wishNotify && (!checkClass || playerClass == DungeonClass.HEALER)) {
                            showTitle("§b${wishNotifyText.getText()}", duration = 2000)
                            KnitChat.modMessage("§b${wishNotifyText.getText()}")
                        }

                        if (ultimateNotify && (!checkClass || playerClass in listOf(DungeonClass.TANK, DungeonClass.ARCHER))) {
                            showTitle("§b${ultimateNotifyText.getText()}", duration = 2000)
                            KnitChat.modMessage("§b${ultimateNotifyText.getText()}")
                        }
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

    private fun String.getText(): String {
        return this.replace("&", "§")
    }
}