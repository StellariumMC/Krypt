package xyz.meowing.krypt.features.general

import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent

@Module
object ScarfSpawnTimer : Feature(
    "scarfSpawnTimers",
    "Scarf spawn timers",
    "Shows spawn timers for Scarf's minions and Scarf in dungeons",
    "General",
    dungeonFloor = listOf(DungeonFloor.F2, DungeonFloor.M2)
) {
    private var time = 0.0
    private var activeTimers = emptyList<TimerData>()

    private data class TimerData(
        val name: String,
        val offset: Double,
        val pos: Vec3
    )

    private val minions = listOf(
        TimerData("§cWarrior", 0.2, Vec3(14.5, 72.5, -3.5)),
        TimerData("§dPriest", 0.3, Vec3(-28.5, 72.5, -3.5)),
        TimerData("§bMage", 0.4, Vec3(14.5, 72.5, -22.5)),
        TimerData("§aArcher", 0.5, Vec3(-28.5, 72.5, -22.5))
    )

    private val boss = listOf(TimerData("§6Scarf", 0.4, Vec3(-7.5, 72.0, -10.5)))

    override fun initialize() {
        createCustomEvent<RenderEvent.World.Last>("render") {
            activeTimers.forEach { timer ->
                val displayTime = time + timer.offset

                if (displayTime > 0) {
                    Render3D.drawString("${timer.name} §e${"%.1f".format(displayTime)}s", timer.pos, it.context.matrixStack())
                }
            }
        }

        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register

            when (event.message.stripped) {
                "[BOSS] Scarf: If you can beat my Undeads, I'll personally grant you the privilege to replace them." -> {
                    time = 7.75
                    activeTimers = minions
                    startTimer()
                }
                "[BOSS] Scarf: Those toys are not strong enough I see." -> {
                    time = 10.0
                    activeTimers = boss
                    startTimer()
                }
            }
        }

        register<LocationEvent.WorldChange> { reset() }
    }

    private fun startTimer() {
        registerEvent("render")
        createTimer(((time + 5) / 0.05).toInt(),
            onTick = {
                time -= 0.05
            },
            onComplete = {
                reset()
            }
        )
    }

    private fun reset() {
        activeTimers = emptyList()
        unregisterEvent("render")
    }
}