    package xyz.meowing.krypt.features.map.render

    import xyz.meowing.krypt.config.ConfigDelegate
    import xyz.meowing.krypt.config.ui.elements.MCColorCode
    import java.awt.Color

    object MapRenderConfig {
        val showPlayerHead by ConfigDelegate<Boolean>("dungeonMap.showPlayerHead")
        val playerHeadsUnder by ConfigDelegate<Boolean>("dungeonMap.playerHeadsUnder")
        val iconClassColors by ConfigDelegate<Boolean>("dungeonMap.iconClassColors")
        val playerIconBorderColor by ConfigDelegate<Color>("dungeonMap.playerIconBorderColor")
        val playerIconSize by ConfigDelegate<Double>("dungeonMap.playerIconSize")
        val playerIconBorderSize by ConfigDelegate<Double>("dungeonMap.playerIconBorderSize")
        val showOwnPlayer by ConfigDelegate<Boolean>("dungeonMap.showOwnPlayer")
        val showPlayerNametags by ConfigDelegate<Boolean>("dungeonMap.showPlayerNametags")
        val showOnlyOwnHeadAsArrow by ConfigDelegate<Boolean>("dungeonMap.showOnlyOwnHeadAsArrow")
        val forcePaul by ConfigDelegate<Boolean>("dungeonMap.forcePaul")

        val healerColor by ConfigDelegate<Color>("dungeonMap.healerColor")
        val mageColor by ConfigDelegate<Color>("dungeonMap.mageColor")
        val berserkColor by ConfigDelegate<Color>("dungeonMap.berserkColor")
        val archerColor by ConfigDelegate<Color>("dungeonMap.archerColor")
        val tankColor by ConfigDelegate<Color>("dungeonMap.tankColor")

        val puzzleCheckmarkMode by ConfigDelegate<Int>("dungeonMap.puzzleCheckmarkMode")
        val normalCheckmarkMode by ConfigDelegate<Int>("dungeonMap.normalCheckmarkMode")
        val checkmarkScale by ConfigDelegate<Double>("dungeonMap.checkmarkScale")
        val roomTextNotClearedColor by ConfigDelegate<MCColorCode>("dungeonMap.roomTextNotClearedColor")
        val roomTextClearedColor by ConfigDelegate<MCColorCode>("dungeonMap.roomTextClearedColor")
        val roomTextSecretsColor by ConfigDelegate<MCColorCode>("dungeonMap.roomTextSecretsColor")
        val roomTextFailedColor by ConfigDelegate<MCColorCode>("dungeonMap.roomTextFailedColor")
        val secretsTextNotClearedColor by ConfigDelegate<MCColorCode>("dungeonMap.secretsTextNotClearedColor")
        val secretsTextClearedColor by ConfigDelegate<MCColorCode>("dungeonMap.secretsTextClearedColor")
        val secretsTextSecretsColor by ConfigDelegate<MCColorCode>("dungeonMap.secretsTextSecretsColor")
        val textShadow by ConfigDelegate<Boolean>("dungeonMap.textShadow")
        val roomLabelScale by ConfigDelegate<Double>("dungeonMap.roomLabelScale")
        val scaleTextToFitRoom by ConfigDelegate<Boolean>("dungeonMap.scaleTextToFitRoom")
        val renderPuzzleIcons by ConfigDelegate<Boolean>("dungeonMap.renderPuzzleIcons")
        val puzzleIconScale by ConfigDelegate<Double>("dungeonMap.puzzleIconScale")
        val tintPuzzleIcons by ConfigDelegate<Boolean>("dungeonMap.tintPuzzleIcons")

        val showClearedRoomCheckmarks by ConfigDelegate<Boolean>("dungeonMap.showClearedRoomCheckmarks")
        val clearedRoomCheckmarkScale by ConfigDelegate<Double>("dungeonMap.clearedRoomCheckmarkScale")

        val normalRoomColor by ConfigDelegate<Color>("dungeonMap.normalRoomColor")
        val puzzleRoomColor by ConfigDelegate<Color>("dungeonMap.puzzleRoomColor")
        val trapRoomColor by ConfigDelegate<Color>("dungeonMap.trapRoomColor")
        val yellowRoomColor by ConfigDelegate<Color>("dungeonMap.yellowRoomColor")
        val bloodRoomColor by ConfigDelegate<Color>("dungeonMap.bloodRoomColor")
        val fairyRoomColor by ConfigDelegate<Color>("dungeonMap.fairyRoomColor")
        val entranceRoomColor by ConfigDelegate<Color>("dungeonMap.entranceRoomColor")

        val changeDoorColorOnOpen by ConfigDelegate<Boolean>("dungeonMap.changeDoorColorOnOpen")
        val normalDoorColor by ConfigDelegate<Color>("dungeonMap.normalDoorColor")
        val witherDoorColor by ConfigDelegate<Color>("dungeonMap.witherDoorColor")
        val bloodDoorColor by ConfigDelegate<Color>("dungeonMap.bloodDoorColor")
        val entranceDoorColor by ConfigDelegate<Color>("dungeonMap.entranceDoorColor")

        val mapInfoUnder by ConfigDelegate<Boolean>("dungeonMap.mapInfoUnder")
        val mapInfoScale by ConfigDelegate<Double>("dungeonMap.mapInfoScale")
        val mapInfoShadow by ConfigDelegate<Boolean>("dungeonMap.infoTextShadow")
        val mapBorder by ConfigDelegate<Boolean>("dungeonMap.mapBorder")
        val mapBorderWidth by ConfigDelegate<Int>("dungeonMap.mapBorderWidth")
        val mapBorderColor by ConfigDelegate<Color>("dungeonMap.mapBorderColor")
        val mapBackgroundColor by ConfigDelegate<Color>("dungeonMap.mapBackgroundColor")

        val bossMapEnabled by ConfigDelegate<Boolean>("dungeonMap.bossMap")
        val scoreMapEnabled by ConfigDelegate<Boolean>("dungeonMap.scoreMap")
    }