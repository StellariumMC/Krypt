package xyz.meowing.krypt.features.general

import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.meowing.knit.api.KnitClient.player
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.Utils.equalsOneOf
import xyz.meowing.krypt.utils.rendering.Render2D
import xyz.meowing.krypt.utils.rendering.Render3D

@Module
object SpringBootsOverlay : Feature(
    "springBootsOverlay",
    "Spring boots overlay",
    "Shows the amount of blocks you can jump",
    "General",
    skyblockOnly = true
) {
    private const val NAME = "Spring Boots Overlay"
    private val pitchList = listOf(
        0.6984127163887024,
        0.8253968358039856,
        0.8888888955116272,
        0.9365079402923584,
        1.047619104385376,
        1.1746032238006592,
        1.317460298538208,
    )

    // thanks od
    private val blocksList = listOf(
        0.0f, 3.0f, 6.5f, 9.0f, 11.5f, 13.5f, 16.0f, 18.0f, 19.0f,
        20.5f, 22.5f, 25.0f, 26.5f, 28.0f, 29.0f, 30.0f, 31.0f, 33.0f,
        34.0f, 35.5f, 37.0f, 38.0f, 39.5f, 40.0f, 41.0f, 42.5f, 43.5f,
        44.0f, 45.0f, 46.0f, 47.0f, 48.0f, 49.0f, 50.0f, 51.0f, 52.0f,
        53.0f, 54.0f, 55.0f, 56.0f, 57.0f, 58.0f, 59.0f, 60.0f, 61.0f
    )

    private var progress = 0
    private var height = 0f

    private val color by config.colorPicker("Color")
    private val render3d by config.switch("Render 3D box", true)
    private val expansion by config.slider("Box expansion", 0.0, 5.0, 0.0, true)

    override fun initialize() {
        HUDManager.register(NAME, "§eHeight: §c44", "springBootsOverlay")

        register<PacketEvent.Received> { event ->
            val packet = event.packet as? ClientboundSoundPacket ?: return@register
            val player = player ?: return@register
            if (progress >= 45) return@register

            val id = packet.sound.value().location
            val feetItem = player.getItemBySlot(EquipmentSlot.FEET).getData(DataTypes.SKYBLOCK_ID)?.skyblockId
            val pitch = packet.pitch

            when {
                SoundEvents.NOTE_BLOCK_PLING.`is`(id) && player.isCrouching && feetItem == "SPRING_BOOTS" -> {
                    if (pitch.toDouble() in pitchList) progress++
                }

                id.equalsOneOf(SoundEvents.FIREWORK_ROCKET_LAUNCH.location, SoundEvents.GENERIC_EAT.value().location) && pitch.equalsOneOf(0.0952381f, 1.6984127f) -> {
                    progress = 0
                    height = 0f
                }
            }

            height = blocksList[progress.coerceIn(blocksList.indices)]
        }

        register<TickEvent.Client> {
            val player = player ?: return@register

            if (
                player.isCrouching &&
                player.getItemBySlot(EquipmentSlot.FEET).getData(DataTypes.SKYBLOCK_ID)?.skyblockId == "SPRING_BOOTS"
            ) return@register

            progress = 0
            height = 0f
        }

        register<RenderEvent.World.Last> { event ->
            if (height == 0f || !render3d) return@register

            player?.position()?.add(0.0, height.toDouble(), 0.0)?.let { pos ->
                val unitBox = AABB.unitCubeFromLowerCorner(pos)
                val box = if (expansion > 0.0) unitBox.inflate(expansion) else unitBox

                Render3D.drawOutlinedBB(
                    box,
                    color,
                    event.context.consumers(),
                    event.context.matrixStack()
                )
            }
        }

        register<GuiEvent.Render.HUD> { event ->
            if (height == 0f) return@register

            val x = HUDManager.getX(NAME)
            val y = HUDManager.getY(NAME)
            val scale = HUDManager.getScale(NAME)
            val decimal = height.color()

            val height = if (decimal.endsWith(".0")) decimal.dropLast(2) else decimal

            Render2D.renderString(event.context, "§eHeight: $height", x, y, scale)
        }
    }

    private fun Float.color(): String {
        return when {
            this <= 13.5 -> "§c"
            this <= 22.5 -> "§e"
            this <= 33.0 -> "§6"
            this <= 43.5 -> "§a"
            else -> "§b"
        } + this
    }
}