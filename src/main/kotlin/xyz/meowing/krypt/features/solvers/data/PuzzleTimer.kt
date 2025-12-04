package xyz.meowing.krypt.features.solvers.data

import com.mojang.serialization.Codec
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.text.buildText
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.data.StoredFile
import xyz.meowing.krypt.config.dsl.Config
import xyz.meowing.krypt.utils.Utils.toTimerFormat
import xyz.meowing.krypt.utils.modMessage

@Module
object PuzzleTimer {
    private val personalBests = StoredFile("features/solvers/puzzleTimes")
    private var pbMap by personalBests.map("puzzles", Codec.STRING, Codec.DOUBLE, emptyMap())

    private var lastPuzzleName: String? = null
    private var lastOldBest: Double? = null

    private val puzzleTimer by Config("Puzzle timer", "Configure puzzle completion time tracking", "Solvers")
    private val useTrueTime by puzzleTimer.switch("Use \"true\" time")

    fun getPersonalBest(puzzleName: String): Double? = pbMap[puzzleName]

    fun submitTime(puzzleName: String, solveTime: Double, trueTime: Double) {
        val completionTime = if (useTrueTime) trueTime else solveTime
        val previousBest = getPersonalBest(puzzleName)
        val message = formatPbMessage(puzzleName, completionTime, previousBest, solveTime, trueTime)

        KnitChat.modMessage(message)

        if (previousBest == null || completionTime < previousBest) {
            lastPuzzleName = puzzleName
            lastOldBest = previousBest
            pbMap = pbMap + (puzzleName to completionTime)
            personalBests.forceSave()
        }
    }

    fun undo() {
        val puzzle = lastPuzzleName ?: return
        val oldBest = lastOldBest

        pbMap = if (oldBest == null) pbMap - puzzle else pbMap + (puzzle to oldBest)

        personalBests.forceSave()

        lastPuzzleName = null
        lastOldBest = null

        KnitChat.modMessage("§7Undid last PB.")
    }

    private fun formatPbMessage(puzzleName: String, time: Double, previousBest: Double?, solveTime: Double, trueTime: Double) = buildText {
        val timeStr = (time / 1000.0).toFloat().toTimerFormat()
        val hoverTime = if (useTrueTime)
            "§7Solve Time: §b${(solveTime / 1000.0).toFloat().toTimerFormat()}"
        else
            "§7Total Time: §b${(trueTime / 1000.0).toFloat().toTimerFormat()}"

        val mainText = when {
            previousBest == null -> {
                "§c$puzzleName §f» §b$timeStr §7(§eNew PB!§7) §8| $hoverTime"
            }

            time < previousBest -> {
                val improvement = ((previousBest - time) / 1000.0).toFloat().toTimerFormat()
                "§c$puzzleName §f» §b$timeStr §7(§a-$improvement §8| §ePB!§7) §8| $hoverTime"
            }

            else -> {
                val difference = ((time - previousBest) / 1000.0).toFloat().toTimerFormat()
                "§c$puzzleName §f» §b$timeStr §7(§c+$difference§7) §8| $hoverTime"
            }
        }

        text(mainText)

        if (previousBest == null || time < previousBest) {
            text(" §8↶") {
                darkGray()
                onHover("§7Click to undo")
                runCommand("/krypt dev undoPB")
            }
        }
    }.toVanilla()
}