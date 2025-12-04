package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDEditor
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.rendering.Render2D
import xyz.meowing.krypt.utils.rendering.Render2D.width

@Module
object RoomSecrets: Feature(
    "roomSecrets",
    "Room secrets HUD",
    "Shows the secrets in the current dungeon room",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Secrets Display"

    init {
        config.button("Edit Position") {
            TickScheduler.Client.post {
                client.setScreen(HUDEditor())
            }
        }
    }

    override fun initialize() {
        HUDManager.registerCustom(NAME, 50,30, this::hudEditorRender, "roomSecrets")

        register<GuiEvent.Render.HUD> {
            if (DungeonAPI.inBoss) return@register
            renderHUD(it.context)
        }
    }

    fun hudEditorRender(context: GuiGraphics) {
        val matrix = context.pose()
        //#if MC >= 1.21.7
        //$$ matrix.pushMatrix()
        //#else
        matrix.pushPose()
        //#endif

        val text1 = "§fSecrets"
        val text2 = "§a7§7/§a7"
        val w1 = text1.width().toFloat()
        val w2 = text2.width().toFloat()

        //#if MC >= 1.21.7
        //$$ matrix.translate(25f, 5f)
        //#else
        matrix.translate(25f, 5f, 0f)
        //#endif

        Render2D.renderString(context, text1, -w1 / 2f, 0f, 1f)
        Render2D.renderString(context, text2, -w2 / 2f, 10f, 1f)

        //#if MC >= 1.21.7
        //$$ matrix.popMatrix()
        //#else
        matrix.popPose()
        //#endif
    }

    private fun renderHUD(context: GuiGraphics) {
        val matrix = context.pose()
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        //#if MC >= 1.21.7
        //$$ matrix.pushMatrix()
        //$$ matrix.translate(x, y)
        //$$ matrix.scale(scale, scale)
        //#else
        matrix.pushPose()
        matrix.translate(x, y, 0f)
        matrix.scale(scale, scale, 1f)
        //#endif

        val text1 = "§fSecrets"
        val text2 = getText()
        val w1 = text1.width().toFloat()
        val w2 = text2.width().toFloat()

        //#if MC >= 1.21.7
        //$$ matrix.translate(25f, 5f)
        //#else
        matrix.translate(25f, 5f, 0f)
        //#endif

        Render2D.renderString(context, text1, -w1 / 2f, 0f, 1f, textStyle = Render2D.TextStyle.DROP_SHADOW)
        Render2D.renderString(context, text2, -w2 / 2f, 10f, 1f, textStyle = Render2D.TextStyle.DROP_SHADOW)

        //#if MC >= 1.21.7
        //$$ matrix.popMatrix()
        //#else
        matrix.popPose()
        //#endif
    }

    private fun getText(): String {
        val room = DungeonAPI.currentRoom
        val found = room?.secretsFound ?: 0
        val total = room?.secrets ?: 0
        var text: String

        if ((found == 0 || found == -1) && total == 0) {
            text = "§7None"
            return text
        }

        val percent = found.toFloat() / total.toFloat()

        text = when {
            percent < 0.5f -> "§c$found§7/§c$total"
            percent <   1f -> "§e$found§7/§e$total"
            else           -> "§a$found§7/§a$total"
        }

        return text
    }
}