package xyz.meowing.krypt.features.alerts

import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature

/**
 * Contains modified code from Noamm's MelodyAlert feature.
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/MelodyAlert.kt)
 */
@Module
object MelodyAlert : Feature(
    "melodyAlert",
    "Melody alert",
    "Alerts your party when you get a melody terminal",
    "Alerts",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val message by config.textInput("Message to send", "I ❤ Melody")

    private var inMelody = false
    private var claySlots = mutableMapOf(
        25 to "$message 1/4",
        34 to "$message 2/4",
        43 to "$message 3/4"
    )

    override fun initialize() {
        register<GuiEvent.Open> { event ->
            if (message.isEmpty()) return@register

            if (event.screen.title.stripped == "Click the button on time!") {
                inMelody = true

                claySlots = mutableMapOf(
                    25 to "$message 1/4",
                    34 to "$message 2/4",
                    43 to "$message 3/4"
                )

                TimeScheduler.schedule(100L) {
                    KnitChat.sendCommand("pc $message")
                }
            }
        }

        register<GuiEvent.Close> {
            if (inMelody) inMelody = false
        }

        register<TickEvent.Client> {
            if (message.isEmpty()) return@register
            if (!inMelody) return@register
            val player = client.player ?: return@register

            val greenClays = claySlots.keys.filter {
                player.containerMenu.getSlot(it).item.`is`(Items.GREEN_TERRACOTTA)
            }

            if (greenClays.isEmpty()) return@register
            val lastClay = greenClays.last()
            val message = claySlots[lastClay] ?: return@register

            KnitChat.sendCommand("pc $message")

            greenClays.forEach { claySlots.remove(it) }
        }
    }
}