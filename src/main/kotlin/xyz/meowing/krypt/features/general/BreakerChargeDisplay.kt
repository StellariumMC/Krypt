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
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.rendering.Render2D
import java.awt.Color

@Module
object BreakerChargeDisplay : Feature(
    "breakerChargeDisplay",
    "Breaker charge display",
    "Displays the charges left and the max charges on your dungeon breaker.",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Breaker Charge Display"
    private val dungeonBreakerRegex = Regex("Charges: (?<current>\\d+)/(?<max>\\d+)⸕")
    private var renderString = ""
    private var charges = 0

    private val renderText by config.switch("Render text")
    private val compact by config.switch("Compact display", true)
    private val outlineBlocks by config.switch("Outline blocks", true)
    private val noChargesColor by config.colorPicker("No charges color", Color(255, 0, 0, 255))
    private val allChargesColor by config.colorPicker("All charges color", Color(0, 255, 0, 255))

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

            val color = if (charges == 0) noChargesColor else allChargesColor

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