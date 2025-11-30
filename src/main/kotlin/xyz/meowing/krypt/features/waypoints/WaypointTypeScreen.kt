package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.BlockPos
import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.screen.KnitScreen
import xyz.meowing.krypt.utils.rendering.Render2D
import org.lwjgl.glfw.GLFW
import xyz.meowing.knit.api.KnitClient.client
import java.awt.Color

class WaypointTypeScreen(private val blockPos: BlockPos) : KnitScreen("Select Waypoint Type") {
    private val types = WaypointType.entries
    private var selectedIndex = 0
    private var hoveredIndex = -1

    private val buttonWidth = 200
    private val buttonHeight = 30
    private val spacing = 10

    override fun onRender(context: GuiGraphics?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        if (context == null) return

        val startY = (height - (types.size * (buttonHeight + spacing))) / 2

        Render2D.drawRect(context, 0, 0, width, height, Color(0, 0, 0, 128))

        Render2D.renderString(context, "Select Waypoint Type", width / 2f - 60, startY - 30f, 1f)

        types.forEachIndexed { index, type ->
            val x = (width - buttonWidth) / 2
            val y = startY + index * (buttonHeight + spacing)

            val isHovered = mouseX in x..(x + buttonWidth) && mouseY in y..(y + buttonHeight)
            hoveredIndex = if (isHovered) index else hoveredIndex

            val color = when {
                selectedIndex == index -> Color(76, 175, 80, 255)
                isHovered -> Color(33, 150, 243, 255)
                else -> Color(66, 66, 66, 255)
            }

            Render2D.drawRect(context, x, y, buttonWidth, buttonHeight, color)

            context.fill(x, y, x + buttonWidth, y + 1, Color.WHITE.rgb)
            context.fill(x, y + buttonHeight - 1, x + buttonWidth, y + buttonHeight, Color.WHITE.rgb)
            context.fill(x, y, x + 1, y + buttonHeight, Color.WHITE.rgb)
            context.fill(x + buttonWidth - 1, y, x + buttonWidth, y + buttonHeight, Color.WHITE.rgb)

            val displayName = type.name.lowercase().replaceFirstChar { it.uppercase() }
            val textWidth = client.font.width(displayName)
            Render2D.renderString(
                context,
                displayName,
                width / 2f - textWidth / 2f,
                y + (buttonHeight - 8) / 2f,
                1f
            )
        }

        val instructionY = startY + types.size * (buttonHeight + spacing) + 20
        val instructionText = "Press ENTER to confirm or ESC to cancel"
        val instructionWidth = client.font.width(instructionText)
        Render2D.renderString(
            context,
            instructionText,
            width / 2f - instructionWidth / 2f,
            instructionY.toFloat(),
            1f,
            Color(170, 170, 170).rgb
        )
    }

    override fun onMouseClick(mouseX: Int, mouseY: Int, button: Int) {
        if (button != 0) return

        val startY = (height - (types.size * (buttonHeight + spacing))) / 2

        types.forEachIndexed { index, _ ->
            val x = (width - buttonWidth) / 2
            val y = startY + index * (buttonHeight + spacing)

            if (mouseX in x..(x + buttonWidth) && mouseY in y..(y + buttonHeight)) {
                selectedIndex = index
                client.setScreen(WaypointTitleScreen(blockPos, types[selectedIndex]))
            }
        }
    }

    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int) {
        when (keyCode) {
            GLFW.GLFW_KEY_UP -> {
                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
            }
            GLFW.GLFW_KEY_DOWN -> {
                selectedIndex = (selectedIndex + 1).coerceAtMost(types.lastIndex)
            }
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                client.setScreen(WaypointTitleScreen(blockPos, types[selectedIndex]))
            }
            GLFW.GLFW_KEY_ESCAPE -> {
                minecraft?.setScreen(null)
            }
        }
    }

    override fun isPauseScreen(): Boolean = false
}