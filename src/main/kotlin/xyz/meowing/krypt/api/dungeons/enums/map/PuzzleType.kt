package xyz.meowing.krypt.api.dungeons.enums.map

import net.minecraft.resources.ResourceLocation
import xyz.meowing.krypt.Krypt

enum class PuzzleType(
    val nameString: String,
    val icon: ResourceLocation,
    var checkmark: Checkmark? = null
) {
    UNKNOWN("???", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/clear/question_mark.png")),
    HIGHER_BLAZE("Higher Blaze", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/blaze.png")),
    LOWER_BLAZE("Lower Blaze", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/blaze.png")),
    BEAMS("Creeper Beams", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/beam.png")),
    WEIRDOS("Three Weirdos", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/weirdos.png")),
    TIC_TAC_TOE("Tic Tac Toe", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/ttt.png")),
    WATER_BOARD("Water Board", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/water.png")),
    TELEPORT_MAZE("Teleport Maze", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/tp_maze.png")),
    BOULDER("Boulder", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/boulder.png")),
    ICE_FILL("Ice Fill", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/ice_fill.png")),
    ICE_PATH("Ice Path", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/ice_path.png")),
    QUIZ("Quiz", ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/puzzles/quiz.png"))
    ;

    companion object {
        fun getPuzzleIcon(puzzleName: String?): ResourceLocation {
            return when (puzzleName) {
                BEAMS.nameString -> BEAMS.icon
                WEIRDOS.nameString -> WEIRDOS.icon
                TIC_TAC_TOE.nameString -> TIC_TAC_TOE.icon
                WATER_BOARD.nameString -> WATER_BOARD.icon
                TELEPORT_MAZE.nameString -> TELEPORT_MAZE.icon
                HIGHER_BLAZE.nameString -> HIGHER_BLAZE.icon
                LOWER_BLAZE.nameString -> LOWER_BLAZE.icon
                BOULDER.nameString -> BOULDER.icon
                ICE_FILL.nameString -> ICE_FILL.icon
                ICE_PATH.nameString -> ICE_PATH.icon
                QUIZ.nameString -> QUIZ.icon
                else -> UNKNOWN.icon
            }
        }
    }
}