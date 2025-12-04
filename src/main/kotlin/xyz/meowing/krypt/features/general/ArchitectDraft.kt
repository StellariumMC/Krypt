package xyz.meowing.krypt.features.general

import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.features.Feature
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.knit.api.text.core.ClickEvent
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.utils.modMessage

@Module
object ArchitectDraft : Feature(
    "architectDraft",
    "Architect draft message",
    "Sends a message in chat for you to get an architect's draft if a puzzle is failed",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val puzzleFailRegex = Regex("^PUZZLE FAIL! (?<player>\\w{1,16}) .+$")
    private val quizFailRegex = Regex("^\\[STATUE] Oruo the Omniscient: (?<player>\\w{1,16}) chose the wrong answer! I shall never forget this moment of misrememberance\\.$")
    private val onlySelf by config.switch("Only for yourself")

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            val text = event.message.stripped

            val name = puzzleFailRegex.findGroup(text, "player")
                ?: quizFailRegex.findGroup(text, "player")
                ?: return@register

            if (name != player?.name?.stripped && onlySelf) return@register

            val archMessage = KnitText
                .literal("§bClick to get Architect's First Draft from Sack.")
                .onClick(ClickEvent.RunCommand("/gfs architect's first draft 1"))

            KnitChat.modMessage(archMessage)
        }
    }
}