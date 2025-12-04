package xyz.meowing.krypt.features.floor7

import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.utils.NumberUtils.format
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.Utils.equalsOneOf

@Module
object NecronHealthDisplay : Feature(
    "necronHealthDisplay",
    "Necron health display",
    "Shows boss health in F7/M7 for the withers",
    "Floor 7",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val displayMode by config.dropdown("Display mode", listOf("Numeric", "Percentage"))

    override fun initialize() {
        register<RenderEvent.BossBar> { event ->
            val name = event.boss.name.stripped
            if (!name.equalsOneOf("Maxor", "Storm", "Goldor", "Necron")) return@register

            val isMaster = DungeonAPI.floor?.isMasterMode ?: return@register
            val maxHealth = when (name) {
                "Maxor" -> if (isMaster) 800_000_000 else 100_000_000
                "Storm" -> if (isMaster) 1_000_000_000 else 400_000_000
                "Goldor" -> if (isMaster) 1_200_000_000 else 750_000_000
                "Necron" -> if (isMaster) 1_400_000_000 else 1_000_000_000
                else -> return@register
            }

            val originalName = event.boss.name.string
            event.boss.name = when (displayMode) {
                1 -> {
                    val percentage = String.format("%.1f", event.boss.progress * 100)
                    val toShow = if (percentage.endsWith(".0")) percentage.dropLast(2) else percentage
                    Component.literal("§4$originalName§r§8 - §r§d$toShow%")
                }

                0 -> {
                    val currentHealth = (event.boss.progress * maxHealth).toLong()
                    val formattedCurrent = format(currentHealth).uppercase()
                    val formattedMax = format(maxHealth).uppercase()
                    Component.literal("§4$originalName§r§8 - §r§a$formattedCurrent§r§8/§r§a$formattedMax §r§c❤")
                }

                else -> event.boss.name
            }
        }
    }
}