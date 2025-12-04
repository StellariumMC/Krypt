package xyz.meowing.krypt.features.map

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.scheduler.TimeScheduler
import xyz.meowing.knit.api.text.KnitText
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.enums.map.RoomClearInfo
import xyz.meowing.krypt.api.dungeons.enums.map.RoomType
import xyz.meowing.krypt.api.dungeons.handlers.DungeonPlayerManager
import xyz.meowing.krypt.api.hypixel.HypixelAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.modMessage

@Module
object DungeonBreakdown : Feature(
    "dungeonBreakdown",
    "Dungeon breakdown",
    "Shows a breakdown of all player's run stats at the end of the run",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun initialize() {
        register<DungeonEvent.End> {
            val totalPlayers = DungeonPlayerManager.players.filterNotNull().size
            if (totalPlayers == 0) return@register

            var updatedCount = 0

            DungeonPlayerManager.players.filterNotNull().forEach { player ->
                if (player.uuid == null) {
                    updatedCount++
                    if (updatedCount >= totalPlayers) sendBreakdown()
                } else {
                    HypixelAPI.fetchSecrets(player.uuid.toString(), cacheMs = 0) { secrets ->
                        player.currSecrets = secrets

                        updatedCount++
                        if (updatedCount >= totalPlayers) sendBreakdown()
                    }
                }
            }
        }
    }

    private fun sendBreakdown() {
        TimeScheduler.schedule(100) {
            KnitChat.modMessage("§fCleared room counts:")

            DungeonPlayerManager.players.forEach { player ->
                if (player == null) return@forEach

                val name = player.name
                val secrets = player.secrets
                val minmax = "${player.minRooms}-${player.maxRooms}"
                val deaths = player.deaths
                val roomLore = buildRoomLore(player)

                val message = KnitText
                    .literal("§7| §b$name §fcleared §b$minmax §frooms | §b$secrets §fsecrets | §b$deaths §fdeaths")
                    .onHover(roomLore)

                KnitChat.fakeMessage(message)
            }
        }
    }

    private fun buildRoomLore(player: DungeonPlayer): String {
        val greenRooms = player.getGreenChecks()
        val whiteRooms = player.getWhiteChecks()
        val visitedGreenNames = greenRooms.keys.toMutableSet()
        val lore = StringBuilder()

        val allRooms = mutableListOf<RoomClearInfo>()
        allRooms.addAll(greenRooms.values)
        allRooms.addAll(whiteRooms.values.filter { it.room.name !in visitedGreenNames })

        allRooms.filter { it.room.type != RoomType.FAIRY }.forEachIndexed { i, info ->
            val room = info.room
            val name = room.name?.takeIf { it != "Default" } ?: room.shape.toString()
            val type = room.type.nickname
            val color = room.type.colorCode
            val checkColor = if (room.name in visitedGreenNames) "a" else "f"
            val time = info.time / 1000.0

            val stackStr = if (info.solo) "" else {
                val others = room.players.filter { it.name != player.name }.map { it.name }
                if (others.isEmpty()) "." else ", Stacked with ${others.joinToString(", ")}."
            }

            val line = "§$color$name §7(§$color$type§7) §7[§$checkColor✔§7]§$color in ${String.format("%.1f", time)}s$stackStr"
            lore.append(if (i == allRooms.lastIndex) line else "$line\n")
        }

        return lore.toString()
    }
}