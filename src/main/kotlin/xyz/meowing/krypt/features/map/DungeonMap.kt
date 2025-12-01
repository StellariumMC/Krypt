package xyz.meowing.krypt.features.map

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.elements.MCColorCode
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.map.render.MapRenderer
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import java.awt.Color

@Module
object DungeonMap : Feature(
    "dungeonMap",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    val defaultMap: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/default_map")
    val markerSelf: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/marker_self")
    val markerOther: ResourceLocation = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "krypt/marker_other")

    private const val NAME = "Dungeon Map"

    override fun addConfig() {
        addPlayerHeadConfig()
        addClassColorConfig()
        addRoomLabelConfig()
        addRoomColorConfig()
        addDoorColorConfig()
        addMapDisplayConfig()
    }

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

    private fun addPlayerHeadConfig() {
        ConfigManager
            .addFeature(
                "Dungeon map",
                "Enables the dungeon map",
                "Map",
                ConfigElement(
                    "dungeonMap",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Show player heads",
                ConfigElement(
                    "dungeonMap.showPlayerHead",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Player heads under text",
                ConfigElement(
                    "dungeonMap.playerHeadsUnder",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Show only own head as arrow",
                ConfigElement(
                    "dungeonMap.showOnlyOwnHeadAsArrow",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Class colored icons",
                ConfigElement(
                    "dungeonMap.iconClassColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Player icon size",
                ConfigElement(
                    "dungeonMap.playerIconSize",
                    ElementType.Slider(0.1, 2.0, 1.0, true)
                )
            )
            .addFeatureOption(
                "Player icon border color",
                ConfigElement(
                    "dungeonMap.playerIconBorderColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Player icon border size",
                ConfigElement(
                    "dungeonMap.playerIconBorderSize",
                    ElementType.Slider(0.0, 0.5, 0.2, true)
                )
            )
            .addFeatureOption(
                "Show own player",
                ConfigElement(
                    "dungeonMap.showOwnPlayer",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Show player nametags",
                ConfigElement(
                    "dungeonMap.showPlayerNametags",
                    ElementType.Switch(true)
                )
            )
    }

    private fun addClassColorConfig() {
        ConfigManager
            .addFeature(
                "Class colors",
                "Configure class icon colors",
                "Map",
                ConfigElement(
                    "dungeonMap.classColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Healer color",
                ConfigElement(
                    "dungeonMap.healerColor",
                    ElementType.ColorPicker(Color(240, 70, 240, 255))
                )
            )
            .addFeatureOption(
                "Mage color",
                ConfigElement(
                    "dungeonMap.mageColor",
                    ElementType.ColorPicker(Color(70, 210, 210, 255))
                )
            )
            .addFeatureOption(
                "Berserk color",
                ConfigElement(
                    "dungeonMap.berserkColor",
                    ElementType.ColorPicker(Color(70, 210, 210, 255))
                )
            )
            .addFeatureOption(
                "Archer color",
                ConfigElement(
                    "dungeonMap.archerColor",
                    ElementType.ColorPicker(Color(254, 223, 0, 255))
                )
            )
            .addFeatureOption(
                "Tank color",
                ConfigElement(
                    "dungeonMap.tankColor",
                    ElementType.ColorPicker(Color(30, 170, 50, 255))
                )
            )
    }

    private fun addRoomLabelConfig() {
        ConfigManager
            .addFeature(
                "Room labels",
                "Configure room label display",
                "Map",
                ConfigElement(
                    "dungeonMap.roomLabels",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Puzzle room",
                ConfigElement(
                    "dungeonMap.puzzleCheckmarkMode",
                    ElementType.Dropdown(
                        listOf("Nothing", "Name Only", "Secrets Only", "Name & Secrets"),
                        0
                    )
                )
            )
            .addFeatureOption(
                "Normal room",
                ConfigElement(
                    "dungeonMap.normalCheckmarkMode",
                    ElementType.Dropdown(
                        listOf("Nothing", "Name Only", "Secrets Only", "Name & Secrets"),
                        0
                    )
                )
            )
            .addFeatureOption(
                "Checkmark scale",
                ConfigElement(
                    "dungeonMap.checkmarkScale",
                    ElementType.Slider(0.5, 2.0, 1.0, true)
                )
            )
            .addFeatureOption(
                "Text shadow",
                ConfigElement(
                    "dungeonMap.textShadow",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Room text not cleared",
                ConfigElement(
                    "dungeonMap.roomTextNotClearedColor",
                    ElementType.MCColorPicker(MCColorCode.GRAY)
                )
            )
            .addFeatureOption(
                "Room text cleared",
                ConfigElement(
                    "dungeonMap.roomTextClearedColor",
                    ElementType.MCColorPicker(MCColorCode.WHITE)
                )
            )
            .addFeatureOption(
                "Room text secrets found",
                ConfigElement(
                    "dungeonMap.roomTextSecretsColor",
                    ElementType.MCColorPicker(MCColorCode.GREEN)
                )
            )
            .addFeatureOption(
                "Room text failed",
                ConfigElement(
                    "dungeonMap.roomTextFailedColor",
                    ElementType.MCColorPicker(MCColorCode.RED)
                )
            )
            .addFeatureOption(
                "Secrets text not cleared",
                ConfigElement(
                    "dungeonMap.secretsTextNotClearedColor",
                    ElementType.MCColorPicker(MCColorCode.GRAY)
                )
            )
            .addFeatureOption(
                "Secrets text cleared",
                ConfigElement(
                    "dungeonMap.secretsTextClearedColor",
                    ElementType.MCColorPicker(MCColorCode.WHITE)
                )
            )
            .addFeatureOption(
                "Secrets text all found",
                ConfigElement(
                    "dungeonMap.secretsTextSecretsColor",
                    ElementType.MCColorPicker(MCColorCode.GREEN)
                )
            )
            .addFeatureOption(
                "Scale text to fit room",
                ConfigElement(
                    "dungeonMap.scaleTextToFitRoom",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Room label scale",
                ConfigElement(
                    "dungeonMap.roomLabelScale",
                    ElementType.Slider(0.5, 2.0, 1.0, true)
                )
            )
            .addFeatureOption(
                "Show room checkmarks",
                ConfigElement(
                    "dungeonMap.showClearedRoomCheckmarks",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Room checkmark scale",
                ConfigElement(
                    "dungeonMap.clearedRoomCheckmarkScale",
                    ElementType.Slider(0.5, 2.0, 1.0, true)
                )
            )
            .addFeatureOption(
                "Render puzzle icons",
                ConfigElement(
                    "dungeonMap.renderPuzzleIcons",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Puzzle icon scale",
                ConfigElement(
                    "dungeonMap.puzzleIconScale",
                    ElementType.Slider(0.5, 2.0, 1.0, true)
                )
            )
    }

    private fun addRoomColorConfig() {
        ConfigManager
            .addFeature(
                "Room colors",
                "Configure room type colors",
                "Map",
                ConfigElement(
                    "dungeonMap.roomColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Normal room",
                ConfigElement(
                    "dungeonMap.normalRoomColor",
                    ElementType.ColorPicker(Color(107, 58, 17, 255))
                )
            )
            .addFeatureOption(
                "Puzzle room",
                ConfigElement(
                    "dungeonMap.puzzleRoomColor",
                    ElementType.ColorPicker(Color(117, 0, 133, 255))
                )
            )
            .addFeatureOption(
                "Trap room",
                ConfigElement(
                    "dungeonMap.trapRoomColor",
                    ElementType.ColorPicker(Color(216, 127, 51, 255))
                )
            )
            .addFeatureOption(
                "Yellow room",
                ConfigElement(
                    "dungeonMap.yellowRoomColor",
                    ElementType.ColorPicker(Color(254, 223, 0, 255))
                )
            )
            .addFeatureOption(
                "Blood room",
                ConfigElement(
                    "dungeonMap.bloodRoomColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Fairy room",
                ConfigElement(
                    "dungeonMap.fairyRoomColor",
                    ElementType.ColorPicker(Color(224, 0, 255, 255))
                )
            )
            .addFeatureOption(
                "Entrance room",
                ConfigElement(
                    "dungeonMap.entranceRoomColor",
                    ElementType.ColorPicker(Color(20, 133, 0, 255))
                )
            )
    }

    private fun addDoorColorConfig() {
        ConfigManager
            .addFeature(
                "Door colors",
                "Configure door type colors",
                "Map",
                ConfigElement(
                    "dungeonMap.doorColors",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Change color on open",
                ConfigElement(
                    "dungeonMap.changeDoorColorOnOpen",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Normal door",
                ConfigElement(
                    "dungeonMap.normalDoorColor",
                    ElementType.ColorPicker(Color(80, 40, 10, 255))
                )
            )
            .addFeatureOption(
                "Wither door",
                ConfigElement(
                    "dungeonMap.witherDoorColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Blood door",
                ConfigElement(
                    "dungeonMap.bloodDoorColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Entrance door",
                ConfigElement(
                    "dungeonMap.entranceDoorColor",
                    ElementType.ColorPicker(Color(0, 204, 0, 255))
                )
            )
    }

    private fun addMapDisplayConfig() {
        ConfigManager
            .addFeature(
                "Map display",
                "Configure map appearance",
                "Map",
                ConfigElement(
                    "dungeonMap.mapDisplay",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Map info under",
                ConfigElement(
                    "dungeonMap.mapInfoUnder",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Info text shadow",
                ConfigElement(
                    "dungeonMap.infoTextShadow",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Map info scale",
                ConfigElement(
                    "dungeonMap.mapInfoScale",
                    ElementType.Slider(0.3, 1.5, 0.6, true)
                )
            )
            .addFeatureOption(
                "Map border",
                ConfigElement(
                    "dungeonMap.mapBorder",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Map border width",
                ConfigElement(
                    "dungeonMap.mapBorderWidth",
                    ElementType.Slider(1.0, 5.0, 2.0, false)
                )
            )
            .addFeatureOption(
                "Map border color",
                ConfigElement(
                    "dungeonMap.mapBorderColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Map background color",
                ConfigElement(
                    "dungeonMap.mapBackgroundColor",
                    ElementType.ColorPicker(Color(0, 0, 0, 100))
                )
            )
            .addFeatureOption(
                "Boss map",
                ConfigElement(
                    "dungeonMap.bossMap",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Score map",
                ConfigElement(
                    "dungeonMap.scoreMap",
                    ElementType.Switch(true)
                )
            )
    }
}