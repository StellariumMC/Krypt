package xyz.meowing.krypt.hud

import xyz.meowing.krypt.managers.config.ConfigManager

class HUDElement(
    val id: String,
    var x: Float,
    var y: Float,
    var width: Int,
    var height: Int,
    var scale: Float = 1f,
    var text: String = "",
    var configKey: String? = null
) {
    fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        return mouseX in x..(x + scaledWidth) && mouseY in y..(y + scaledHeight)
    }

    fun isEnabled(): Boolean {
        return configKey?.let {
            ConfigManager.getConfigValue(it) as? Boolean ?: false
        } ?: true
    }
}
