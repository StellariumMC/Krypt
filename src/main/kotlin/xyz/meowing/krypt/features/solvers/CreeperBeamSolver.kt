package xyz.meowing.krypt.features.solvers

import net.minecraft.world.level.block.Blocks
import net.minecraft.core.BlockPos
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.WorldEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.solvers.data.PuzzleTimer
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.WorldUtils
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

/**
 * Contains modified code from Noamm's creeper beams solver.
 *
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/solvers/puzzles/CreeperBeamSolver.kt)
 */
@Module
object CreeperBeamSolver : Feature(
    "creeperBeamSolver",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private data class BeamPair(val start: BlockPos, val end: BlockPos, val color: Color)

    private val beamSolutions = mutableListOf<Pair<List<Int>, List<Int>>>()
    private val currentSolve = mutableListOf<BeamPair>()

    private var inCreeperBeams = false
    private var roomCenter = BlockPos(-1, -1, -1)
    private var rotation = 0

    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null

    private val colorPool = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
        Color.CYAN, Color.ORANGE, Color.WHITE, Color.MAGENTA
    )

    private val showLines by ConfigDelegate<Boolean>("creeperBeamSolver.showLines")
    private val phaseThrough by ConfigDelegate<Boolean>("creeperBeamSolver.phase")
    private val removeOnClick by ConfigDelegate<Boolean>("creeperBeamSolver.removeOnClick")

    init {
        NetworkUtils.fetchJson<List<List<List<Int>>>>(
            url = "https://raw.githubusercontent.com/StellariumMC/zen-data/refs/heads/main/solvers/CreeperBeamsSolver.json",
            onSuccess = { beamsList ->
                beamSolutions.clear()
                beamsList.forEach { beamPair ->
                    beamSolutions.add(Pair(beamPair[0], beamPair[1]))
                }
                Krypt.LOGGER.info("Loaded Creeper beam solutions.")
            },
            onError = { error ->
                Krypt.LOGGER.error("Caught error while trying to load Creeper Beam solutions: $error")
            }
        )
    }

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Creeper beam solver",
                "Highlights beam pairs in Creeper Beams room",
                "Solvers",
                ConfigElement(
                    "creeperBeamSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Show lines",
                ConfigElement(
                    "creeperBeamSolver.showLines",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Phase through walls",
                ConfigElement(
                    "creeperBeamSolver.phase",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Remove on click",
                ConfigElement(
                    "creeperBeamSolver.removeOnClick",
                    ElementType.Switch(true)
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Creeper Beams") return@register

            inCreeperBeams = true
            rotation = 360 - (event.new.rotation.degrees) + 180

            roomCenter = ScanUtils.getRoomCenter(event.new)
            trueTimeStarted = System.currentTimeMillis()

            solve()
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inCreeperBeams && event.new.name != "Creeper Beams") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<RenderEvent.World.Last> { event ->
            if (!inCreeperBeams) return@register

            currentSolve.forEach { (start, end, color) ->
                if (!isBeamBlock(start) || !isBeamBlock(end)) return@forEach

                Render3D.drawSpecialBB(start, color, event.context.consumers(), event.context.matrixStack(), phaseThrough)
                Render3D.drawSpecialBB(end, color, event.context.consumers(), event.context.matrixStack(), phaseThrough)

                if (showLines) {
                    val startVec = start.center
                    val endVec = end.center
                    Render3D.drawLine(startVec, endVec, 1f, color, event.context.consumers(), event.context.matrixStack())
                }
            }
        }

        register<WorldEvent.BlockUpdate> { event ->
            if (!inCreeperBeams || !removeOnClick) return@register

            if (event.old.block == Blocks.SEA_LANTERN && event.new.block != Blocks.SEA_LANTERN) {
                if (timeStarted == null) timeStarted = System.currentTimeMillis()

                currentSolve.removeIf { it.start == event.pos || it.end == event.pos }

                if (currentSolve.isEmpty()) {
                    val trueTime = trueTimeStarted ?: return@register
                    val startTime = timeStarted ?: return@register

                    val solveTime = (System.currentTimeMillis() - startTime).toDouble()
                    val totalTime = (System.currentTimeMillis() - trueTime).toDouble()

                    PuzzleTimer.submitTime("Creeper Beams", solveTime, totalTime)
                    reset()
                }
            }
        }
    }

    private fun solve() {
        var colorIndex = 0
        beamSolutions.forEach { (start, end) ->
            if (colorIndex >= colorPool.size) return@forEach

            val startPos = ScanUtils.getRealCoord(
                BlockPos(start[0], start[1], start[2]),
                roomCenter,
                rotation
            )
            val endPos = ScanUtils.getRealCoord(
                BlockPos(end[0], end[1], end[2]),
                roomCenter,
                rotation
            )

            if (!isBeamBlock(startPos) || !isBeamBlock(endPos)) return@forEach
            currentSolve.add(BeamPair(startPos, endPos, colorPool[colorIndex++]))
        }
    }

    private fun isBeamBlock(pos: BlockPos): Boolean {
        val block = WorldUtils.getBlockStateAt(pos.x, pos.y, pos.z)?.block
        return block == Blocks.PRISMARINE || block == Blocks.SEA_LANTERN
    }

    private fun reset() {
        inCreeperBeams = false
        currentSolve.clear()
        roomCenter = BlockPos(-1, -1, -1)
        rotation = 0
        trueTimeStarted = null
        timeStarted = null
    }
}