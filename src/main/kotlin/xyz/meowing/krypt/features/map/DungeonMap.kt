package xyz.meowing.krypt.features.map

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.dsl.Config
import xyz.meowing.krypt.config.ui.elements.MCColorCode
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.map.render.MapRenderer
import xyz.meowing.krypt.hud.HUDManager
import java.awt.Color

@Module
object DungeonMap : Feature(
    "dungeonMap",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    val defaultMap: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/default_map.png")
    val markerSelf: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/marker_self.png")
    val markerOther: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/marker_other.png")

    private const val NAME = "Dungeon Map"

    private val dungeonMap by Config("Toggle map", "Enables the dungeon map", "Map", false)
    val forcePaul by dungeonMap.switch("Force paul")

    private val playerIcons by Config("Player icons", "Configure player icon appearance and behavior", "Map", true)
    val showPlayerHead by playerIcons.switch("Show player heads", true)
    val showOnlyOwnHeadAsArrow by playerIcons.switch("Only own head as arrow")
    val showOwnPlayer by playerIcons.switch("Show own player", true)
    val showPlayerNametags by playerIcons.switch("Show player nametags", true)
    val playerHeadsUnder by playerIcons.switch("Player heads under text", true)
    val iconClassColors by playerIcons.switch("Class colored icons", true)
    val playerIconSize by playerIcons.slider("Player icon size", 1.0, 0.1, 2.0, true)
    val playerIconBorderSize by playerIcons.slider("Player icon border size", 0.2, 0.0, 0.5, true)
    val playerIconBorderColor by playerIcons.colorPicker("Player icon border color", Color(0, 0, 0, 255))

    private val classColors by Config("Class colors", "Configure class icon colors", "Map", true)
    val healerColor by classColors.colorPicker("Healer color", Color(240, 70, 240, 255))
    val mageColor by classColors.colorPicker("Mage color", Color(70, 210, 210, 255))
    val berserkColor by classColors.colorPicker("Berserk color", Color(70, 210, 210, 255))
    val archerColor by classColors.colorPicker("Archer color", Color(254, 223, 0, 255))
    val tankColor by classColors.colorPicker("Tank color", Color(30, 170, 50, 255))

    private val roomLabels by Config("Room labels", "Configure room name and secret display", "Map", true)
    val puzzleCheckmarkMode by roomLabels.dropdown("Puzzle room display", listOf("Nothing", "Name Only", "Secrets Only", "Name & Secrets"), 0)
    val normalCheckmarkMode by roomLabels.dropdown("Normal room display", listOf("Nothing", "Name Only", "Secrets Only", "Name & Secrets"), 0)
    val roomLabelScale by roomLabels.slider("Room label scale", 1.0, 0.5, 2.0, true)
    val scaleTextToFitRoom by roomLabels.switch("Scale text to fit room", true)
    val textShadow by roomLabels.switch("Text shadow")
    val showClearedRoomCheckmarks by roomLabels.switch("Show room checkmarks", true)
    val clearedRoomCheckmarkScale by roomLabels.slider("Room checkmark scale", 1.0, 0.5, 2.0, true)
    val checkmarkScale by roomLabels.slider("Checkmark scale", 1.0, 0.5, 2.0, true)

    private val roomTextColors by Config("Room text colors", "Configure room name text colors", "Map", true)
    val roomTextNotClearedColor by roomTextColors.mcColorPicker("Not cleared", MCColorCode.GRAY)
    val roomTextClearedColor by roomTextColors.mcColorPicker("Cleared", MCColorCode.WHITE)
    val roomTextSecretsColor by roomTextColors.mcColorPicker("Secrets found", MCColorCode.GREEN)
    val roomTextFailedColor by roomTextColors.mcColorPicker("Failed", MCColorCode.RED)

    private val secretsTextColors by Config("Secrets text colors", "Configure secret count text colors", "Map", true)
    val secretsTextNotClearedColor by secretsTextColors.mcColorPicker("Not cleared", MCColorCode.GRAY)
    val secretsTextClearedColor by secretsTextColors.mcColorPicker("Cleared", MCColorCode.WHITE)
    val secretsTextSecretsColor by secretsTextColors.mcColorPicker("All found", MCColorCode.GREEN)

    private val puzzleIcons by Config("Puzzle icons", "Configure puzzle icon display", "Map", true)
    val renderPuzzleIcons by puzzleIcons.switch("Render puzzle icons", true)
    val tintPuzzleIcons by puzzleIcons.switch("Tint puzzle icons", true)
    val puzzleIconScale by puzzleIcons.slider("Puzzle icon scale", 1.0, 0.5, 2.0, true)

    private val roomColors by Config("Room colors", "Configure room type colors", "Map", true)
    val normalRoomColor by roomColors.colorPicker("Normal room", Color(107, 58, 17, 255))
    val puzzleRoomColor by roomColors.colorPicker("Puzzle room", Color(117, 0, 133, 255))
    val trapRoomColor by roomColors.colorPicker("Trap room", Color(216, 127, 51, 255))
    val yellowRoomColor by roomColors.colorPicker("Yellow room", Color(254, 223, 0, 255))
    val bloodRoomColor by roomColors.colorPicker("Blood room", Color(255, 0, 0, 255))
    val fairyRoomColor by roomColors.colorPicker("Fairy room", Color(224, 0, 255, 255))
    val entranceRoomColor by roomColors.colorPicker("Entrance room", Color(20, 133, 0, 255))

    private val doorColors by Config("Door colors", "Configure door type colors", "Map", true)
    val changeDoorColorOnOpen by doorColors.switch("Change color on open")
    val normalDoorColor by doorColors.colorPicker("Normal door", Color(80, 40, 10, 255))
    val witherDoorColor by doorColors.colorPicker("Wither door", Color(0, 0, 0, 255))
    val bloodDoorColor by doorColors.colorPicker("Blood door", Color(255, 0, 0, 255))
    val entranceDoorColor by doorColors.colorPicker("Entrance door", Color(20, 133, 0, 255))

    private val mapDisplay by Config("Map display", "Configure overall map appearance", "Map", true)
    val bossMap by mapDisplay.switch("Boss map", true)
    val scoreMap by mapDisplay.switch("Score map", true)
    val mapBorder by mapDisplay.switch("Map border")
    val mapBorderWidth by mapDisplay.slider("Map border width", 2, 1, 5)
    val mapBorderColor by mapDisplay.colorPicker("Map border color", Color(0, 0, 0, 255))
    val mapBackgroundColor by mapDisplay.colorPicker("Map background color", Color(0, 0, 0, 100))

    private val mapInfo by Config("Map info display", "Configure info text below map", "Map", true)
    val mapInfoUnder by mapInfo.switch("Show map info", true)
    val infoTextShadow by mapInfo.switch("Info text shadow")
    val mapInfoScale by mapInfo.slider("Map info scale", 0.6, 0.3, 1.5, true)

    override fun initialize() {
        HUDManager.registerCustom(
            NAME,
            148,
            158,
            { MapRenderer.renderPreview(it, 0f, 0f) },
            "dungeonMap"
        )

        register<GuiEvent.Render.HUD> { event -> renderMap(event.context) }
    }

    private fun renderMap(context: GuiGraphics) {
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        MapRenderer.render(context, x, y, scale)
    }
}