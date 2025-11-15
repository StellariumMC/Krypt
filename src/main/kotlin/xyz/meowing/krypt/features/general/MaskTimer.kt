@file:Suppress("UNUSED")

package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.utils.extentions.createSkull
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.knit.api.events.EventCall
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.api.skyblock.PetTracker
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.types.ElementType
import xyz.meowing.krypt.events.EventBus
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HudManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Utils.removeFormatting
import xyz.meowing.krypt.utils.rendering.Render2D

@Module
object MaskTimer : Feature(
    "maskTimers",
    island = SkyBlockIsland.THE_CATACOMBS
) {

    private const val NAME = "MaskTimers"
    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Mask Timers",
                "Mask Timers",
                "General",
                ConfigElement(
                    "maskTimers",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Send Chat Message",
                ConfigElement(
                    "maskTimers.message",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Always Display Timer",
                ConfigElement(
                    "maskTimers.alwaysDisplay",
                    ElementType.Switch(true)
                )
            )
    }

    private enum class Mask {
        SPIRIT,
        BONZO,
        PHOENIX
    }

    private var BonzoTicks = 0.0
    private var SpiritTicks = 0.0
    private var PhoenixTicks = 0.0

    private var hasSpiritMask = false
    private var hasBonzoMask = false
    private val BonzoRegex = "^Your (?:. )?Bonzo's Mask saved your life!$".toRegex()

    private val message by ConfigDelegate<Boolean>("maskTimers.message")
    private val alwaysDisplay by ConfigDelegate<Boolean>("maskTimers.alwaysDisplay")

    private val tickCall: EventCall = EventBus.register<TickEvent.Server> {
        updateTimers()
        updateHelmetStatus()
    }

    data class MaskData(val mask: ItemStack, val timeStr: String, val color: String, val isWearing: Boolean)

    private val SpiritMask: ItemStack =
        createSkull("eyJ0aW1lc3RhbXAiOjE1MDUyMjI5OTg3MzQsInByb2ZpbGVJZCI6IjBiZTU2MmUxNzIyODQ3YmQ5MDY3MWYxNzNjNjA5NmNhIiwicHJvZmlsZU5hbWUiOiJ4Y29vbHgzIiwic2lnbmF0dXJlUmVxdWlyZWQiOnRydWUsInRleHR1cmVzIjp7IlNLSU4iOnsibWV0YWRhdGEiOnsibW9kZWwiOiJzbGltIn0sInVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWJiZTcyMWQ3YWQ4YWI5NjVmMDhjYmVjMGI4MzRmNzc5YjUxOTdmNzlkYTRhZWEzZDEzZDI1M2VjZTlkZWMyIn19fQ==")
    private val BonzoMask: ItemStack =
        createSkull("eyJ0aW1lc3RhbXAiOjE1ODc5MDgzMDU4MjYsInByb2ZpbGVJZCI6IjJkYzc3YWU3OTQ2MzQ4MDI5NDI4MGM4NDIyNzRiNTY3IiwicHJvZmlsZU5hbWUiOiJzYWR5MDYxMCIsInNpZ25hdHVyZVJlcXVpcmVkIjp0cnVlLCJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTI3MTZlY2JmNWI4ZGEwMGIwNWYzMTZlYzZhZjYxZThiZDAyODA1YjIxZWI4ZTQ0MDE1MTQ2OGRjNjU2NTQ5YyJ9fX0=")
    private val Phoenix: ItemStack =
        createSkull("ewogICJ0aW1lc3RhbXAiIDogMTY0Mjg2NTc3MTM5MSwKICAicHJvZmlsZUlkIiA6ICJiYjdjY2E3MTA0MzQ0NDEyOGQzMDg5ZTEzYmRmYWI1OSIsCiAgInByb2ZpbGVOYW1lIiA6ICJsYXVyZW5jaW8zMDMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjZiMWI1OWJjODkwYzljOTc1Mjc3ODdkZGUyMDYwMGM4Yjg2ZjZiOTkxMmQ1MWE2YmZjZGIwZTRjMmFhM2M5NyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9")

    private val previewMasks = listOf(
        MaskData(BonzoMask, "153.4s", "§c", true),
        MaskData(SpiritMask, "12.4s", "§c", false),
        MaskData(Phoenix, "60.0s", "§c", true)
    )

    override fun initialize() {
        HudManager.registerCustom(NAME, 60, 57, this::editorRender, "maskTimers")

        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            val text = event.message.string.removeFormatting()

            when {
                text.matches(BonzoRegex) -> {
                    BonzoTicks =
                        (maxOf(180.0, (if (text.contains("⚚")) 180.0 else 360.0) - DungeonAPI.cataLevel * 3.6) * 20)
                    tickCall.register()

                    if (message)
                        sendChatMessage(Mask.BONZO)
                }

                text == "Second Wind Activated! Your Spirit Mask saved your life!" -> {
                    SpiritTicks = DungeonAPI.getMageReduction(30.0) * 20
                    tickCall.register()

                    if (message)
                        sendChatMessage(Mask.SPIRIT)
                }

                text == ("Your Phoenix Pet saved you from certain death!") -> {
                    PhoenixTicks = 1200.0
                    tickCall.register()

                    if (message)
                        sendChatMessage(Mask.PHOENIX)
                }
            }
        }

        register<GuiEvent.RenderHUD> { event ->
            render(event.context)
        }

        register<LocationEvent.WorldChange> {
            BonzoTicks = 0.0
            SpiritTicks = 0.0
        }
    }

    private fun sendChatMessage(mask: Mask) {
        when (mask) {
            Mask.BONZO -> {
                KnitChat.sendMessage("/pc Bonzo proceed")
            }

            Mask.SPIRIT -> {
                KnitChat.sendMessage("/pc Spirit proceed")
            }

            Mask.PHOENIX -> {
                KnitChat.sendMessage("/pc Phoenix proceed")
            }
        }
    }

    private fun updateTimers() {
        if (BonzoTicks > 0) BonzoTicks--
        if (SpiritTicks > 0) SpiritTicks--
        if (PhoenixTicks > 0) PhoenixTicks--
        if (BonzoTicks <= 0 && SpiritTicks <= 0 && PhoenixTicks <= 0) tickCall.unregister()
    }

    private fun updateHelmetStatus() {
        hasSpiritMask = checkHelmet("Spirit Mask")
        hasBonzoMask = checkHelmet("Bonzo's Mask")
    }

    private fun checkHelmet(name: String): Boolean {
        return player?.inventory?.getItem(39)?.displayName?.string?.contains(name) == true
    }

    private fun render(context: GuiGraphics) {
        val activeMasks = getActiveMasks()
        if (activeMasks.isEmpty()) return

        val x = HudManager.getX(NAME)
        val y = HudManager.getY(NAME)
        val scale = HudManager.getScale(NAME)
        drawHUD(context, x, y, scale, activeMasks)
    }

    private fun editorRender(context: GuiGraphics) = drawHUD(context, 0f, 0f, 1f, previewMasks)

    private fun drawHUD(context: GuiGraphics, x: Float, y: Float, scale: Float, masks: List<MaskData>) {
        val iconSize = 16f * scale
        val spacing = 2f * scale
        var currentY = y

        masks.forEach { maskData ->
            val textY = currentY + (iconSize - 8f) / 2f
            val separatorColor = if (maskData.isWearing) "§a" else "§7"

            Render2D.renderItem(context, maskData.mask, x, currentY, scale)
            Render2D.renderStringWithShadow(
                context,
                "${separatorColor}| ${maskData.color}${maskData.timeStr}",
                x + iconSize + spacing,
                textY,
                scale
            )

            currentY += iconSize + spacing

        }
    }

    private fun getActiveMasks(): List<MaskData> {
        val masks = mutableListOf<MaskData>()

        val bonzoTimer = if (BonzoTicks > 0) String.format("%.1fs", BonzoTicks / 20.0) else "AVAILABLE"
        if (bonzoTimer != "AVAILABLE" || alwaysDisplay)
            masks.add(MaskData(BonzoMask, bonzoTimer, if (BonzoTicks > 0) "§c" else "§a", hasBonzoMask))

        val spiritTimer = if (SpiritTicks > 0) String.format("%.1fs", SpiritTicks / 20.0) else "AVAILABLE"
        if (spiritTimer != "AVAILABLE" || alwaysDisplay)
            masks.add(MaskData(SpiritMask, spiritTimer, if (SpiritTicks > 0) "§c" else "§a", hasSpiritMask))

        val phoenixTimer = if (PhoenixTicks > 0) String.format("%.1fs", PhoenixTicks / 20.0) else "AVAILABLE"
        if (phoenixTimer != "AVAILABLE" || alwaysDisplay)
            masks.add(
                MaskData(
                    Phoenix,
                    phoenixTimer,
                    if (PhoenixTicks > 0) "§c" else "§a",
                    PetTracker.name.contains("phoenix", true)
                )
            )
        return masks
    }
}