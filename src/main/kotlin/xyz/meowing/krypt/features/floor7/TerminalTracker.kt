package xyz.meowing.krypt.features.floor7

import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.find
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.modMessage

@Module
object TerminalTracker : Feature(
    "terminalTracker",
    "Terminal tracker",
    "Tracks terminal/device/lever completions in dungeons",
    "Floor 7",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val completed: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    private val pattern = Regex("""^(?<user>\w{1,16}) (?:activated|completed) a (?<type>\w+)! \(\d/\d\)$""")

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            val message = event.message.stripped

            when {
                message == "The Core entrance is opening!" -> {
                    completed.toList().forEach { (user, data) ->
                        KnitChat.modMessage("§b$user§7 - §b${data["lever"] ?: 0} §flevers §7| §b${data["terminal"] ?: 0} §fterminals §7| §b${data["device"] ?: 0} §fdevices")
                    }
                    completed.clear()
                }

                pattern.find(message, "user", "type") { (user, type) ->
                    if (type in listOf("terminal", "lever", "device")) {
                        completed.getOrPut(user) { mutableMapOf() }[type] = (completed[user]?.get(type) ?: 0) + 1
                    }
                } -> {}
            }
        }

        register<LocationEvent.WorldChange> {
            completed.clear()
        }
    }
}