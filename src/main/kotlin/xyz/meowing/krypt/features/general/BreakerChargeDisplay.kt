package xyz.meowing.krypt.features.general

import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.phys.shapes.CollisionContext
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import tech.thatgravyboat.skyblockapi.utils.extentions.getRawLore
import tech.thatgravyboat.skyblockapi.utils.extentions.parseFormattedInt
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.rendering.Render2D
import java.awt.Color

@Module
object BreakerChargeDisplay : Feature(
    "breakerChargeDisplay",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Breaker Charge Display"
    private val dungeonBreakerRegex = Regex("Charges: (?<current>\\d+)/(?<max>\\d+)⸕")
    private var renderString = ""
    private var charges = 0

    private val compact by ConfigDelegate<Boolean>("breakerChargeDisplay.compact")
    private val renderText by ConfigDelegate<Boolean>("breakerChargeDisplay.renderText")
    private val outlineBlocks by ConfigDelegate<Boolean>("breakerChargeDisplay.outlineBlocks")
    private val emptyColor by ConfigDelegate<Color>("breakerChargeDisplay.noChargesColor")
    private val fullColor by ConfigDelegate<Color>("breakerChargeDisplay.allChargesColor")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Breaker charge display",
                "Displays the charges left and the max charges on your dungeon breaker.",
                "General",
                ConfigElement(
                    "breakerChargeDisplay",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Render text",
                ConfigElement(
                    "breakerChargeDisplay.renderText",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Compact display",
                ConfigElement(
                    "breakerChargeDisplay.compact",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Outline blocks",
                ConfigElement(
                    "breakerChargeDisplay.outlineBlocks",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "No charges color",
                ConfigElement(
                    "breakerChargeDisplay.noChargesColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "All charges color",
                ConfigElement(
                    "breakerChargeDisplay.allChargesColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 255))
                )
            )
    }

    override fun initialize() {
        HUDManager.register(NAME, "§c⸕§e20", "breakerChargeDisplay.renderText")

        register<PacketEvent.Received> { event ->
            val packet = event.packet as? ClientboundContainerSetSlotPacket ?: return@register
            val stack = packet.item ?: return@register

            if (stack.getData(DataTypes.SKYBLOCK_ID)?.skyblockId?.equals("DUNGEONBREAKER") == false) return@register

            dungeonBreakerRegex.anyMatch(stack.getRawLore(), "current", "max") { (current, max) ->
                charges = current.parseFormattedInt()

                val colorCode = when {
                    charges >= 15 -> "§a"
                    charges >= 10 -> "§b"
                    else -> "§c"
                }

                renderString = if (compact) "§c⸕$colorCode$charges" else "§bCharges: ${colorCode}${charges}§7/§a${max}§c⸕"
            }
        }

        register<GuiEvent.Render.HUD> { event ->
            if (!renderText) return@register
            if (renderString.isEmpty()) return@register

            val x = HUDManager.getX(NAME)
            val y = HUDManager.getY(NAME)
            val scale = HUDManager.getScale(NAME)

            Render2D.renderString(event.context, renderString, x, y, scale)
        }

        register<RenderEvent.World.BlockOutline> { event ->
            if (!outlineBlocks) return@register
            if (KnitPlayer.heldItem?.getData(DataTypes.SKYBLOCK_ID)?.skyblockId != "DUNGEONBREAKER") return@register

            val blockPos = event.context.blockPos() ?: return@register
            val blockState = event.context.blockState() ?: return@register
            val matrixStack = event.context.matrixStack() ?: return@register
            val consumers = event.context.consumers()
            val camera = client.gameRenderer.mainCamera
            val blockShape = blockState.getShape(
                EmptyBlockGetter.INSTANCE,
                blockPos,
                CollisionContext.of(camera.entity)
            )
            if (blockShape.isEmpty) return@register

            val camPos = camera.position
            event.cancel()

            val color = if (charges == 0) emptyColor else fullColor

            ShapeRenderer.renderShape(
                matrixStack,
                consumers.getBuffer(RenderType.lines()),
                blockShape,
                blockPos.x - camPos.x,
                blockPos.y - camPos.y,
                blockPos.z - camPos.z,
                color.rgb
            )
        }

        register<LocationEvent.WorldChange> {
            renderString = ""
            charges = 20
        }
    }
}