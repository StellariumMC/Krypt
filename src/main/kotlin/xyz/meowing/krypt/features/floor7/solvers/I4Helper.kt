package xyz.meowing.krypt.features.floor7.solvers

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonPhase
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.WorldEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object I4Helper : Feature(
    "i4Helper",
    "I4 helper",
    "Highlights blocks for the 4th device",
    "Floor 7",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val pressurePlate = BlockPos(63, 127, 35)
    private val gridPositions = listOf(
        BlockPos(68, 130, 50), BlockPos(66, 130, 50), BlockPos(64, 130, 50),
        BlockPos(68, 128, 50), BlockPos(66, 128, 50), BlockPos(64, 128, 50),
        BlockPos(68, 126, 50), BlockPos(66, 126, 50), BlockPos(64, 126, 50)
    )

    private val completedTargets = mutableSetOf<AABB>()
    private var activeTarget: AABB? = null

    private val completedColor by config.colorPicker("Completed target color", Color(255, 0, 0, 127))
    private val activeColor by config.colorPicker("Active target color", Color(0, 255, 0, 127))
    private val renderThroughWalls by config.switch("Render through walls", true)

    override fun initialize() {
        register<WorldEvent.BlockUpdate> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register

            if (event.pos == pressurePlate && event.new.block == Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE) {
                val power = event.new.getValue(BlockStateProperties.POWER)
                val oldPower = event.old.getValue(BlockStateProperties.POWER)

                if (power < 1 || oldPower == 0) {
                    reset()
                    return@register
                }
            }

            if (!gridPositions.contains(event.pos)) return@register

            val targetBox = AABB(event.pos)

            when (event.old.block) {
                Blocks.EMERALD_BLOCK if event.new.block == Blocks.BLUE_TERRACOTTA -> {
                    completedTargets.add(targetBox)
                    if (activeTarget == targetBox) activeTarget = null
                }

                Blocks.BLUE_TERRACOTTA if event.new.block == Blocks.EMERALD_BLOCK -> {
                    completedTargets.remove(targetBox)
                    activeTarget = targetBox
                }
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register

            completedTargets.forEach { box ->
                Render3D.drawFilledBB(
                    box,
                    completedColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = renderThroughWalls
                )
            }

            activeTarget?.let { box ->
                Render3D.drawFilledBB(
                    box,
                    activeColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = renderThroughWalls
                )
            }
        }

        register<LocationEvent.WorldChange> { reset() }
    }

    private fun reset() {
        completedTargets.clear()
        activeTarget = null
    }
}