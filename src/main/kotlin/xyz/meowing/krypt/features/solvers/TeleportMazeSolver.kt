package xyz.meowing.krypt.features.solvers

import net.minecraft.world.level.block.Blocks
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.core.BlockPos
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
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
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Contains modified code from Noamm's teleport maze solver.
 *
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/solvers/puzzles/TeleportMazeSolver.kt)
 */
@Module
object TeleportMazeSolver : Feature(
    "tpMazeSolver",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private class TpPad(
        val pos: BlockPos,
        var cellX: Int = 0,
        var cellZ: Int = 0,
        var totalAngle: Double = 0.0,
        var blacklisted: Boolean = false
    )

    private data class Cell(val xIndex: Int, val zIndex: Int) {
        val pads = mutableSetOf<TpPad>()
        fun addPad(pad: TpPad) { pads += pad }
    }

    private var minX: Int? = null
    private var minZ: Int? = null
    private var cells: List<Cell>? = null
    private var orderedPads: MutableList<TpPad>? = null
    private var inTpMaze = false

    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null

    private val correctPadColor by ConfigDelegate<Color>("tpMazeSolver.correctColor")
    private val wrongPadColor by ConfigDelegate<Color>("tpMazeSolver.wrongColor")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Teleport maze solver",
                "Highlights correct teleport pads in order",
                "Solvers",
                ConfigElement(
                    "tpMazeSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Correct pad color",
                ConfigElement(
                    "tpMazeSolver.correctColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 127))
                )
            )
            .addFeatureOption(
                "Wrong pad color",
                ConfigElement(
                    "tpMazeSolver.wrongColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 127))
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Teleport Maze") return@register

            val center = ScanUtils.getRoomCenter(event.new)

            val rotation = 360 - event.new.rotation.degrees
            val pos1 = ScanUtils.getRealCoord(BlockPos(0, 69, - 3), center, rotation)

            if (client.level?.getBlockState(pos1)?.block != Blocks.END_PORTAL_FRAME) {
                inTpMaze = false
                return@register
            }

            inTpMaze = true
            trueTimeStarted = System.currentTimeMillis()

            val pads = mutableListOf<TpPad>()
            for (dx in 0..31) {
                for (dz in 0..31) {
                    val pos = BlockPos(center.x + dx - 16, 69, center.z + dz - 16)
                    if (client.level?.getBlockState(pos)?.block != Blocks.END_PORTAL_FRAME) continue
                    pads += TpPad(pos)
                }
            }

            minX = pads.minOfOrNull { it.pos.x }
            minZ = pads.minOfOrNull { it.pos.z }
            cells = List(9) { i -> Cell(i / 3, i % 3) }

            for (pad in pads) {
                pad.cellX = (pad.pos.x - minX!!) / 8
                pad.cellZ = (pad.pos.z - minZ!!) / 8
                val index = pad.cellX * 3 + pad.cellZ
                cells!![index].addPad(pad)
            }
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inTpMaze && event.new.name != "Teleport Maze") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<RenderEvent.World.Last> { event ->
            val c = cells ?: return@register
            val top = orderedPads?.takeIf { it.size >= 2 }?.take(2) ?: return@register
            if (top[0].totalAngle == top[1].totalAngle) return@register

            Render3D.drawSpecialBB(
                top[0].pos,
                correctPadColor,
                event.context.consumers(),
                event.context.matrixStack(),
                phase = true
            )

            c.forEach { cell ->
                cell.pads.filter { it.blacklisted }.forEach { pad ->
                    Render3D.drawSpecialBB(
                        pad.pos,
                        wrongPadColor,
                        event.context.consumers(),
                        event.context.matrixStack(),
                        phase = false
                    )
                }
            }
        }

        register<PacketEvent.Received> { event ->
            if (cells == null) return@register
            val packet = event.packet as? ClientboundPlayerPositionPacket ?: return@register
            val pos = packet.change.position
            if (pos.x % 0.5 != 0.0 || pos.y != 69.5 || pos.z % 0.5 != 0.0) return@register

            val oldPad = getPadNear(player!!.x, player!!.z) ?: return@register
            val newPad = getPadNear(pos.x, pos.z) ?: return@register

            if (timeStarted == null) timeStarted = System.currentTimeMillis()

            if (isPadInStartOrEndCell(newPad)) {
                cells!!.forEach { cell ->
                    cell.pads.forEach {
                        it.blacklisted = false
                        it.totalAngle = 0.0
                    }
                }

                val trueTime = trueTimeStarted ?: return@register
                val startTime = timeStarted ?: return@register

                val solveTime = (System.currentTimeMillis() - startTime).toDouble()
                val totalTime = (System.currentTimeMillis() - trueTime).toDouble()

                PuzzleTimer.submitTime("Teleport Maze", solveTime, totalTime)
                reset()
                return@register
            }

            newPad.blacklisted = true
            oldPad.blacklisted = true

            calcPadAngles(pos.x, pos.z, packet.change.yRot())
        }
    }

    private fun calcPadAngles(x: Double, z: Double, yaw: Float) {
        orderedPads = mutableListOf()
        for (cell in cells ?: return) {
            for (pad in cell.pads) {
                if (isPadInStartOrEndCell(pad) || pad.blacklisted) continue
                val padVec = Vector3(pad.pos.x + 0.5 - x, 0.0, pad.pos.z + 0.5 - z)
                pad.totalAngle += Vector3.fromPitchYaw(0.0, yaw.toDouble()).getAngleDeg(padVec)
                orderedPads?.add(pad)
            }
        }
        orderedPads?.sortBy { it.totalAngle }
    }

    private fun reset() {
        minX = null
        minZ = null
        cells = null
        orderedPads = null
        inTpMaze = false
        trueTimeStarted = null
        timeStarted = null
    }

    private fun getCellAt(x: Int, z: Int): Cell? {
        val minX = minX ?: return null
        val minZ = minZ ?: return null
        if (x < minX || x > minX + 23 || z < minZ || z > minZ + 23) return null
        val cx = (x - minX) / 8
        val cz = (z - minZ) / 8
        return cells?.find { it.xIndex == cx && it.zIndex == cz }
    }

    private fun getPadNear(x: Double, z: Double): TpPad? {
        val cell = getCellAt(x.toInt(), z.toInt()) ?: return null
        return cell.pads.find {
            manhattanDistance(x, z, it.pos.x.toDouble(), it.pos.z.toDouble()) <= 3.0
        }
    }

    private fun manhattanDistance(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        return abs(x1 - x2) + abs(z1 - z2)
    }

    private fun isPadInStartOrEndCell(pad: TpPad): Boolean {
        val c = cells ?: return false
        if (c.getOrNull(4)?.pads?.contains(pad) == true) return true
        for (cell in c) {
            if (cell != c[4] && cell.pads.size == 1 && pad in cell.pads) return true
        }
        return false
    }

    private data class Vector3(val x: Double, val y: Double, val z: Double) {
        fun getAngleDeg(other: Vector3): Double {
            val dot = x * other.x + y * other.y + z * other.z
            val mag1 = sqrt(x * x + y * y + z * z)
            val mag2 = sqrt(other.x * other.x + other.y * other.y + other.z * other.z)
            return Math.toDegrees(acos((dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)))
        }

        companion object {
            fun fromPitchYaw(pitch: Double, yaw: Double): Vector3 {
                val pitchRad = Math.toRadians(pitch)
                val yawRad = Math.toRadians(yaw)
                val x = -sin(yawRad) * cos(pitchRad)
                val y = -sin(pitchRad)
                val z = cos(yawRad) * cos(pitchRad)
                return Vector3(x, y, z)
            }
        }
    }
}