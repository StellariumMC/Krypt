package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDEditor
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object RoomName: Feature(
    "roomName",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    val chroma by ConfigDelegate<Boolean>("roomName.chroma")
    private const val NAME = "Room Name"

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Room name HUD",
                "Displays the current rooms name",
                "General",
                ConfigElement(
                    "roomName",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Chroma room name",
                ConfigElement(
                    "roomName.chroma",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption("HudEditor",
                ConfigElement(
                    "roomName.hudEditor",
                    ElementType.Button("Edit Position") {
                        TickScheduler.Client.post {
                            client.execute { client.setScreen(HUDEditor()) }
                        }
                    }
                )
            )
    }

    override fun initialize() {
        HUDManager.register(NAME, "No Room Found", "roomName")
        register<GuiEvent.Render.HUD> { renderHud(it.context) }
    }

    private fun renderHud(context: GuiGraphics) {
        if (DungeonAPI.inBoss) return

        val text = "${if (chroma) "§z" else ""}${DungeonAPI.currentRoom?.name ?: "No Room Found"}"
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        Render2D.renderString(context, text, x, y, scale)
    }
}