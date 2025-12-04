package xyz.meowing.krypt.features.floor7.solvers

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient.player
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonPhase
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.events.core.WorldEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils
import xyz.meowing.krypt.utils.modMessage
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object SimonSaysSolver : Feature(
    "simonSaysSolver",
    "Simons-Says solver",
    "Tracks the solution for Simon-Says in F7/M7",
    "Floor 7",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val sequence = mutableListOf<BlockPos>()
    private val startPos = BlockPos(110, 121, 91)
    private var currentStep = 0

    private val blockWrong by config.switch("Block wrong clicks")
    private val blockWrongType by config.dropdown("Block when", listOf("Always", "When crouching", "Not crouching"))
    private val firstColor by config.colorPicker("First color", Color(0, 255, 0, 127))
    private val secondColor by config.colorPicker("Second color",Color(255, 255, 0, 127) )
    private val thirdColor by config.colorPicker("Third color", Color(255, 165, 0, 127))

    private var ticks = -1
    private var canBreak = false

    override fun initialize() {
        register<LocationEvent.WorldChange> {
            reset()
        }

        register<TickEvent.Server> {
            if (ticks > 0) ticks--
        }

        register<ChatEvent.Receive> { event ->
            val message = event.message.stripped
            if (message == "[BOSS] Goldor: Who dares trespass into my domain?") {
                ticks = 0
                canBreak = false
            }
        }

        register<WorldEvent.BlockUpdate> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register

            if (
                event.pos == startPos &&
                event.new.block == Blocks.STONE_BUTTON &&
                event.new.getValue(BlockStateProperties.POWERED)
            ) return@register

            if (event.pos.y !in 120..123 || event.pos.z !in 92..95) return@register

            when (event.pos.x) {
                111 -> {
                    if (
                        event.new.block == Blocks.SEA_LANTERN &&
                        event.old.block == Blocks.OBSIDIAN &&
                        event.pos !in sequence
                    ) {
                        sequence.add(event.pos.immutable())
                        ticks = 12
                        canBreak = true
                    }
                }

                110 -> {
                    if (
                        event.old.block == Blocks.STONE_BUTTON &&
                        event.new.getValue(BlockStateProperties.POWERED)
                    ) {
                        currentStep = sequence.indexOf(event.pos.east()) + 1
                        if (currentStep >= sequence.size) reset()
                    }

                    if (
                        event.old.block == Blocks.STONE_BUTTON &&
                        event.new.isAir &&
                        ticks <= 0 &&
                        canBreak
                    ) {
                        TitleUtils.showTitle("§cSS Broke", duration = 1000)
                        KnitChat.modMessage("§cSS Broke")
                        canBreak = false
                    }
                }
            }
        }

        register<EntityEvent.Packet.Metadata> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register
            val entity = event.entity as? ItemEntity ?: return@register
            val item = entity.item ?: return@register
            if (item.item != Items.STONE_BUTTON) return@register

            val buttonPos = entity.blockPosition().east()
            val idx = sequence.indexOf(buttonPos)

            when (idx) {
                2 if sequence.size == 3 -> sequence.removeFirst()
                0 if sequence.size == 2 -> sequence.reverse()
            }
        }

        register<PacketEvent.Sent> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register
            val hitPos = packet.hitResult.blockPos

            if (hitPos == startPos) {
                reset()
                return@register
            }

            val shouldBlock = when (blockWrongType) {
                0 -> true
                1 -> player?.isShiftKeyDown == true
                2 -> player?.isShiftKeyDown == false
                else -> false
            } && blockWrong

            if (
                shouldBlock &&
                hitPos.x == 110 &&
                hitPos.y in 120..123 &&
                hitPos.z in 92..95 &&
                hitPos.east() != sequence.getOrNull(currentStep)
            ) event.cancel()
        }

        register<RenderEvent.World.Last> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3 || currentStep >= sequence.size) return@register

            sequence.drop(currentStep).forEachIndexed { offset, blockPos ->
                val color = when (offset) {
                    0 -> firstColor
                    1 -> secondColor
                    else -> thirdColor
                }

                val box = AABB(
                    blockPos.x + 0.1,
                    blockPos.y + 0.5,
                    blockPos.z + 0.5,
                    blockPos.x - 0.1,
                    blockPos.y + 0.5,
                    blockPos.z + 0.5
                )

                Render3D.drawSpecialBB(
                    box,
                    color,
                    event.context.consumers(),
                    event.context.matrixStack()
                )
            }
        }
    }

    private fun reset() {
        sequence.clear()
        canBreak = false
        currentStep = 0
        ticks = 0
    }
}