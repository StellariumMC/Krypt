package xyz.meowing.krypt.features.solvers

import net.minecraft.world.level.block.Blocks
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.level.EmptyBlockGetter
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils.getRealCoord
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.solvers.data.PuzzleTimer
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

/**
 * Contains modified code from Noamm's boulder solver.
 *
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/solvers/puzzles/BoulderSolver.kt)
 */
@Module
object BoulderSolver : Feature(
    "boulderSolver",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private data class BoulderBox(val box: BlockPos, val clickPos: BlockPos, val render: BlockPos)

    private val boulderSolutions = mutableMapOf<String, List<List<Double>>>()
    private var currentSolution = mutableListOf<BoulderBox>()
    private val bottomLeftBox = BlockPos(-9, 65, -9)

    private var inBoulder = false
    private var roomCenter = BlockPos(-1, -1, -1)
    private var rotation = 0

    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null

    private val boxColor by ConfigDelegate<Color>("boulderSolver.boxColor")
    private val clickColor by ConfigDelegate<Color>("boulderSolver.clickColor")
    private val showAll by ConfigDelegate<Boolean>("boulderSolver.showAll")

    init {
        NetworkUtils.fetchJson<Map<String, List<List<Double>>>>(
            url = "https://raw.githubusercontent.com/StellariumMC/zen-data/refs/heads/main/solvers/BoulderSolver.json",
            onSuccess = {
                boulderSolutions.putAll(it)
                Krypt.LOGGER.info("Loaded Boulder solutions.")
            },
            onError = { error ->
                Krypt.LOGGER.error("Caught error while trying to load Boulder solutions: $error")
            }
        )
    }

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Boulder solver",
                "Shows which boulders to click in order",
                "Solvers",
                ConfigElement(
                    "boulderSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Box color",
                ConfigElement(
                    "boulderSolver.boxColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 127))
                )
            )
            .addFeatureOption(
                "Click color",
                ConfigElement(
                    "boulderSolver.clickColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 255))
                )
            )
            .addFeatureOption(
                "Show all boxes",
                ConfigElement(
                    "boulderSolver.showAll",
                    ElementType.Switch(false)
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Boulder") return@register

            inBoulder = true
            rotation = 360 - (event.new.rotation.degrees) + 180
            roomCenter = ScanUtils.getRoomCenter(event.new)
            trueTimeStarted = System.currentTimeMillis()

            solve()
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inBoulder && event.new.name != "Boulder") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<RenderEvent.World.Last> { event ->
            if (!inBoulder || currentSolution.isEmpty()) return@register

            val world = client.level ?: return@register
            val boxes = if (showAll) currentSolution else listOf(currentSolution.first())

            boxes.forEach { box ->
                val boxArea = AABB(
                    box.box.x - 1.0, box.box.y - 1.0, box.box.z - 1.0,
                    box.box.x + 2.0, box.box.y + 2.0, box.box.z + 2.0
                )

                Render3D.drawSpecialBB(
                    boxArea,
                    boxColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = true
                )

                Render3D.drawFilledShapeVoxel(
                    getVoxelShape(box.clickPos, world).move(box.clickPos),
                    clickColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = true
                )
            }
        }

        register<PacketEvent.Sent> { event ->
            if (!inBoulder) return@register

            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register
            val blockPos = packet.hitResult.blockPos
            val block = client.level?.getBlockState(blockPos)?.block ?: return@register

            when (block) {
                Blocks.OAK_WALL_SIGN, Blocks.STONE_BUTTON -> {
                    currentSolution.find { it.clickPos == blockPos }?.let {
                        currentSolution.remove(it)
                        if (timeStarted == null) timeStarted = System.currentTimeMillis()
                    }
                }

                Blocks.CHEST -> {
                    val trueTime = trueTimeStarted ?: return@register
                    val startTime = timeStarted ?: return@register

                    val solveTime = (System.currentTimeMillis() - startTime).toDouble()
                    val totalTime = (System.currentTimeMillis() - trueTime).toDouble()

                    PuzzleTimer.submitTime("Boulder", solveTime, totalTime)
                    reset()
                }
            }
        }
    }

    private fun solve() {
        val (sx, sy, sz) = bottomLeftBox.let { Triple(it.x, it.y, it.z) }
        var pattern = ""

        for (z in 0..5) {
            for (x in 0..6) {
                val pos = getRealCoord(BlockPos(sx + x * 3, sy, sz + z * 3), roomCenter, rotation)
                val block = client.level?.getBlockState(pos)?.block
                pattern += if (block == Blocks.AIR) "0" else "1"
            }
        }

        currentSolution = boulderSolutions[pattern]?.map { sol ->
            val box = getRealCoord(BlockPos(sol[0].toInt(), sy, sol[1].toInt()), roomCenter, rotation)
            val clickPos = getRealCoord(BlockPos(sol[2].toInt(), sy, sol[3].toInt()), roomCenter, rotation)
            val render = getRealCoord(BlockPos(sol[4].toInt(), sy, sol[5].toInt()), roomCenter, rotation)
            BoulderBox(box, clickPos, render)
        }?.toMutableList() ?: mutableListOf()
    }

    private fun reset() {
        inBoulder = false
        roomCenter = BlockPos(-1, -1, -1)
        rotation = 0
        currentSolution.clear()
        trueTimeStarted = null
        timeStarted = null
    }

    private fun getVoxelShape(pos: BlockPos, world: ClientLevel): VoxelShape {
        return world.getBlockState(pos).getShape(
            EmptyBlockGetter.INSTANCE,
            pos
        )
    }
}