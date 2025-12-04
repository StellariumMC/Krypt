package xyz.meowing.krypt.api.dungeons.handlers

import tech.thatgravyboat.skyblockapi.utils.extentions.parseRomanOrArabic
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.handlers.ScoreCalculator.deathCount
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.TablistEvent
import kotlin.text.get

/**
 * Inspired by Skyblocker's implementation.
 * Original File: [GitHub](https://github.com/SkyblockerMod/Skyblocker/blob/master/src/main/java/de/hysky/skyblocker/skyblock/dungeon/secrets/DungeonPlayerManager.java)
 */
@Module
object DungeonPlayerManager {
    val playerTabPattern = Regex("\\[\\d+] (?:\\[[A-Za-z]+] )?(?<name>[A-Za-z0-9_]+) (?:.+ )?\\((?<class>\\S+) ?(?<level>[LXVI0]+)?\\)")
    val playerGhostPattern = Regex(" ☠ (?<name>[A-Za-z0-9_]+) .+ became a ghost\\.")

    val players = Array<DungeonPlayer?>(5) { null }

    init {
        EventBus.registerIn<TablistEvent.Change>(SkyBlockIsland.THE_CATACOMBS) { event ->
            val firstColumn = event.new.firstOrNull() ?: return@registerIn

            var playerIndex = 0

            for (line in firstColumn) {
                if (playerIndex >= 5) break

                val stripped = line.stripped
                val match = playerTabPattern.find(stripped) ?: continue

                val name = match.groups["name"]?.value ?: continue
                val classStr = match.groups["class"]?.value ?: "EMPTY"
                val levelStr = match.groups["level"]?.value

                val clazz = DungeonClass.from(classStr)
                val level = levelStr?.parseRomanOrArabic()

                if (players[playerIndex] != null && players[playerIndex]!!.name == name) {
                    players[playerIndex]!!.dungeonClass = clazz
                    players[playerIndex]!!.classLevel = level
                    players[playerIndex]!!.dead = classStr == "DEAD"
                } else {
                    players[playerIndex] = DungeonPlayer(name, clazz, level)
                    players[playerIndex]!!.dead = classStr == "DEAD"
                }

                playerIndex++
            }

            while (playerIndex < 5) {
                players[playerIndex] = null
                playerIndex++
            }
        }

        EventBus.registerIn<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { onDeath(it.message.string) }
    }

    private fun onDeath(text: String) {
        val match = playerGhostPattern.find(text) ?: return

        var name = match.groups["name"]?.value ?: return
        if (name == "You") KnitPlayer.player?.let { name = it.name.stripped }

        val player = getPlayer(name) ?: return
        player.dead = true
        player.deaths++
        deathCount++

        EventBus.post(DungeonEvent.Player.Death(player))
    }

    fun getPlayer(name: String): DungeonPlayer? {
        return players
            .filterNotNull()
            .firstOrNull { it.name == name }
    }

    fun reset() {
        players.fill(null)
    }
}