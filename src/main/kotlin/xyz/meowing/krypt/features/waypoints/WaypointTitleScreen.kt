package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.BlockPos
import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.screen.KnitScreen
import xyz.meowing.krypt.utils.rendering.Render2D
import org.lwjgl.glfw.GLFW
import xyz.meowing.knit.api.KnitClient.client
import java.awt.Color

class WaypointTitleScreen(
    private val blockPos: BlockPos,
    private val type: WaypointType
) : KnitScreen("Enter Waypoint Title") {
    
    private val textBuffer = StringBuilder()
    private var cursorBlink = 0

    override fun tick() {
        cursorBlink++
    }

    override fun onRender(context: GuiGraphics?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        if (context == null) return
        
        Render2D.drawRect(context, 0, 0, width, height, Color(0, 0, 0, 128))
        
        val titleText = "Enter title (optional)"
        val titleWidth = client.font.width(titleText)
        Render2D.renderString(context, titleText, width / 2f - titleWidth / 2f, height / 2f - 40, 1f)
        
        val boxWidth = 300
        val boxHeight = 30
        val boxX = (width - boxWidth) / 2
        val boxY = height / 2
        
        Render2D.drawRect(context, boxX, boxY, boxWidth, boxHeight, Color(30, 30, 30, 255))
        
        context.fill(boxX, boxY, boxX + boxWidth, boxY + 2, Color(76, 175, 80).rgb)
        context.fill(boxX, boxY + boxHeight - 2, boxX + boxWidth, boxY + boxHeight, Color(76, 175, 80).rgb)
        context.fill(boxX, boxY, boxX + 2, boxY + boxHeight, Color(76, 175, 80).rgb)
        context.fill(boxX + boxWidth - 2, boxY, boxX + boxWidth, boxY + boxHeight, Color(76, 175, 80).rgb)
        
        val text = textBuffer.toString()
        val displayText = if (cursorBlink % 40 < 20) "$text|" else text
        
        Render2D.renderString(context, displayText, boxX + 10f, boxY + 10f, 1f)
        
        val instructionText = "Press ENTER to confirm or ESC to skip"
        val instructionWidth = client.font.width(instructionText)
        Render2D.renderString(
            context,
            instructionText,
            width / 2f - instructionWidth / 2f,
            boxY + 50f,
            1f,
            Color(170, 170, 170).rgb
        )
    }
    
    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int) {
        when (keyCode) {
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                RouteRecorder.addWaypoint(type, blockPos, textBuffer.toString().takeIf { it.isNotBlank() })
                minecraft?.setScreen(null)
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                RouteRecorder.addWaypoint(type, blockPos, null)
                minecraft?.setScreen(null)
            }
            GLFW.GLFW_KEY_BACKSPACE -> {
                if (textBuffer.isNotEmpty()) {
                    textBuffer.deleteCharAt(textBuffer.lastIndex)
                }
            }
            else -> {
                if (typedChar.isLetterOrDigit() || typedChar == ' ' || typedChar in "!@#\$%^&*()_+-=[]{}|;:',.<>?/") {
                    if (textBuffer.length < 30) {
                        textBuffer.append(typedChar)
                    }
                }
            }
        }
    }
    
    override fun isPauseScreen(): Boolean = false
}