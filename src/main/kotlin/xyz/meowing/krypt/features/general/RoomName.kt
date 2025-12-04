package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.elements.MCColorCode
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDEditor
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object RoomName: Feature(
    "roomName",
    "Room name HUD",
    "Displays the current rooms name",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Room Name"
    private val color by config.mcColorPicker("Color", MCColorCode.BLUE)

    init {
        config.button("Edit Position") {
            TickScheduler.Client.post {
                client.setScreen(HUDEditor())
            }
        }
    }

    override fun initialize() {
        HUDManager.register(NAME, "No Room Found", "roomName")
        register<GuiEvent.Render.HUD> { renderHud(it.context) }
    }

    private fun renderHud(context: GuiGraphics) {
        if (DungeonAPI.inBoss) return

        val text = "${color.code}${DungeonAPI.currentRoom?.name ?: "No Room Found"}"
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        Render2D.renderString(context, text, x, y, scale)
    }
}