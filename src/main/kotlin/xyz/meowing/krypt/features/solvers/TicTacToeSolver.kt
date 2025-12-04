package xyz.meowing.krypt.features.solvers

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.MapItem
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.level.EmptyBlockGetter
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.solvers.data.PuzzleTimer
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color
import kotlin.experimental.and
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Contains modified code from Noamm's tic-tac-toe solver.
 *
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/solvers/puzzles/TicTacToeSolver.kt)
 */
@Module
object TicTacToeSolver : Feature(
    "ticTacToeSolver",
    "Tic-Tac-Toe solver",
    "Shows the best move using minimax algorithm",
    "Solvers",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private var inTicTacToe = false
    private var roomCenter: BlockPos? = null
    private var boundingBox: VoxelShape? = null
    private var blockPos: BlockPos? = null

    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null

    private val boxColor by config.colorPicker("Box color", Color(0, 255, 0, 127))

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Tic Tac Toe") return@register

            inTicTacToe = true
            roomCenter = ScanUtils.getRoomCenter(event.new)
            trueTimeStarted = System.currentTimeMillis()

            TickScheduler.Server.schedule(2) {
                scanBoard()
            }
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inTicTacToe && event.new.name != "Tic Tac Toe") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<PacketEvent.Received> { event ->
            if (!inTicTacToe) return@register
            val packet = event.packet as? ClientboundAddEntityPacket ?: return@register
            if (packet.type != EntityType.ITEM_FRAME) return@register

            if (timeStarted == null) timeStarted = System.currentTimeMillis()

            TickScheduler.Server.schedule(5) {
                scanBoard()
            }
        }

        register<PacketEvent.Sent> { event ->
            if (!inTicTacToe) return@register
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register
            val pos = packet.hitResult.blockPos

            if (client.level?.getBlockState(pos)?.block == Blocks.CHEST) {
                val trueTime = trueTimeStarted ?: return@register
                val startTime = timeStarted ?: return@register

                val solveTime = (System.currentTimeMillis() - startTime).toDouble()
                val totalTime = (System.currentTimeMillis() - trueTime).toDouble()

                PuzzleTimer.submitTime("Tic Tac Toe", solveTime, totalTime)
                reset()
            }
        }

        register<RenderEvent.World.Last> { event ->
            boundingBox?.let { box ->
                Render3D.drawFilledShapeVoxel(
                    box.move(blockPos),
                    boxColor,
                    event.context.consumers(),
                    event.context.matrixStack()
                )
            }
        }
    }

    private fun scanBoard() {
        val center = roomCenter ?: return
        val world = KnitClient.world ?: return

        val aabb = AABB(
            center.x - 9.0, 65.0, center.z - 9.0,
            center.x + 9.0, 73.0, center.z + 9.0
        )

        val itemFrames = world.getEntitiesOfClass(ItemFrame::class.java, aabb) { true }
        val itemFramesWithMaps = itemFrames.filter { frame ->
            val item = frame.item ?: return@filter false
            if (item.item !is MapItem) return@filter false
            MapItem.getSavedData(item, world) != null
        }

        if (itemFramesWithMaps.size == 8) {
            reset()
            return
        }

        if (itemFramesWithMaps.size % 2 == 0) return

        val board = Array(3) { CharArray(3) }
        var leftmostRow: BlockPos? = null
        var sign = 1
        var facing = 'X'

        for (itemFrame in itemFramesWithMaps) {
            val map = itemFrame.item ?: continue
            val mapData = (map.item as? MapItem)?.let { MapItem.getSavedData(map, world) } ?: continue

            var row = 0
            sign = if (itemFrame.direction.stepX != 0) {
                if (itemFrame.direction.stepX > 0) 1 else -1
            } else {
                if (itemFrame.direction.stepZ > 0) 1 else -1
            }

            val itemFramePos = BlockPos(
                floor(itemFrame.x).toInt(),
                floor(itemFrame.y).toInt(),
                floor(itemFrame.z).toInt()
            )

            for (i in 2 downTo 0) {
                val realI = i * sign
                var blockPos = itemFramePos

                if (itemFrame.x % 0.5 == 0.0) {
                    blockPos = itemFramePos.offset(realI, 0, 0)
                } else if (itemFrame.z % 0.5 == 0.0) {
                    blockPos = itemFramePos.offset(0, 0, realI)
                    facing = 'Z'
                }

                val block = client.level?.getBlockState(blockPos)?.block
                if (block == Blocks.STONE_BUTTON || block == Blocks.AIR) {
                    leftmostRow = blockPos
                    row = i
                    break
                }
            }

            val column = when (itemFrame.y) {
                72.5 -> 0
                71.5 -> 1
                70.5 -> 2
                else -> continue
            }

            val colorInt = (mapData.colors[8256] and 255.toByte()).toInt()
            when (colorInt) {
                114 -> board[column][row] = 'X'
                33 -> board[column][row] = 'O'
            }
        }

        val moveIndex = getBestMove(board) - 1
        if (moveIndex < 0) return

        leftmostRow?.let { pos ->
            val drawX = (if (facing == 'X') pos.x - sign * (moveIndex % 3) else pos.x).toDouble()
            val drawY = 72.0 - floor((moveIndex / 3).toDouble())
            val drawZ = (if (facing == 'Z') pos.z - sign * (moveIndex % 3) else pos.z).toDouble()

            blockPos = BlockPos(drawX.toInt(), drawY.toInt(), drawZ.toInt())

            boundingBox = world.getBlockState(blockPos).getShape(
                EmptyBlockGetter.INSTANCE,
                blockPos
            )
        }
    }

    private fun getBestMove(board: Array<CharArray>): Int {
        val moves = mutableMapOf<Int, Int>()

        for (row in board.indices) {
            for (col in board[row].indices) {
                if (board[row][col] != '\u0000') continue

                board[row][col] = 'O'
                val score = minimax(board, false, 0)
                board[row][col] = '\u0000'
                moves[row * 3 + col + 1] = score
            }
        }

        return moves.maxByOrNull { it.value }?.key ?: 0
    }

    private fun minimax(board: Array<CharArray>, max: Boolean, depth: Int): Int {
        val score = getBoardRanking(board)
        if (score == 10 || score == -10) return score
        if (!hasMovesLeft(board)) return 0

        if (max) {
            var bestScore = -1000
            for (row in 0..2) {
                for (col in 0..2) {
                    if (board[row][col] == '\u0000') {
                        board[row][col] = 'O'
                        bestScore = max(bestScore, minimax(board, false, depth + 1))
                        board[row][col] = '\u0000'
                    }
                }
            }
            return bestScore - depth
        } else {
            var bestScore = 1000
            for (row in 0..2) {
                for (col in 0..2) {
                    if (board[row][col] == '\u0000') {
                        board[row][col] = 'X'
                        bestScore = min(bestScore, minimax(board, true, depth + 1))
                        board[row][col] = '\u0000'
                    }
                }
            }
            return bestScore + depth
        }
    }

    private fun getBoardRanking(board: Array<CharArray>): Int {
        for (row in 0..2) {
            if (board[row][0] == board[row][1] && board[row][0] == board[row][2]) {
                if (board[row][0] == 'X') return -10
                else if (board[row][0] == 'O') return 10
            }
        }

        for (col in 0..2) {
            if (board[0][col] == board[1][col] && board[0][col] == board[2][col]) {
                if (board[0][col] == 'X') return -10
                else if (board[0][col] == 'O') return 10
            }
        }

        if (board[0][0] == board[1][1] && board[0][0] == board[2][2]) {
            if (board[0][0] == 'X') return -10
            else if (board[0][0] == 'O') return 10
        }

        if (board[0][2] == board[1][1] && board[0][2] == board[2][0]) {
            if (board[0][2] == 'X') return -10
            else if (board[0][2] == 'O') return 10
        }

        return 0
    }

    private fun hasMovesLeft(board: Array<CharArray>): Boolean {
        for (row in board) {
            for (col in row) {
                if (col == '\u0000') return true
            }
        }
        return false
    }

    private fun reset() {
        inTicTacToe = false
        boundingBox = null
        roomCenter = null
        trueTimeStarted = null
        timeStarted = null
    }
}