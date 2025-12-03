package xyz.meowing.krypt.utils.rendering

import net.minecraft.world.level.block.entity.SkullBlockEntity
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.client.resources.PlayerSkin
import net.minecraft.world.item.ItemStack
import net.minecraft.network.chat.Component
import net.minecraft.util.CommonColors
import tech.thatgravyboat.skyblockapi.utils.extentions.stripColor
import xyz.meowing.knit.api.KnitClient.client
import java.awt.Color
import java.util.Optional
import java.util.UUID

//#if MC >= 1.21.8
//$$ import net.minecraft.client.renderer.RenderPipelines
//#endif

//#if MC >= 1.21.9
//$$ import com.mojang.authlib.GameProfile
//#endif

object Render2D {
    enum class TextStyle {
        DROP_SHADOW,
        DEFAULT
    }

    fun renderString(
        context: GuiGraphics,
        text: Component,
        x: Float,
        y: Float,
        scale: Float,
        colors: Int = -1,
        textStyle: TextStyle = TextStyle.DEFAULT
    ) {
        //#if MC >= 1.21.7
        //$$ context.pose().pushMatrix()
        //$$ context.pose().translate(x, y)
        //$$ context.pose().scale(scale, scale)
        //#else
        context.pose().pushPose()
        context.pose().translate(x, y, 0f)
        context.pose().scale(scale, scale, 1f)
        //#endif

        when (textStyle) {
            TextStyle.DROP_SHADOW -> {
                context.drawString(client.font, text, 0, 0, colors, true)
            }

            TextStyle.DEFAULT -> {
                context.drawString(client.font, text, 0, 0, colors, false)
            }
        }

        //#if MC >= 1.21.7
        //$$ context.pose().popMatrix()
        //#else
        context.pose().popPose()
        //#endif
    }

    fun renderString(
        context: GuiGraphics,
        text: String,
        x: Float,
        y: Float,
        scale: Float,
        colors: Int = -1,
        textStyle: TextStyle = TextStyle.DEFAULT
    ) = renderString(context, Component.literal(text), x, y, scale, colors, textStyle)

    fun renderStringWithShadow(context: GuiGraphics, text: String, x: Float, y: Float, scale: Float, colors: Int = CommonColors.WHITE) {
        renderString(context, text, x, y, scale, colors, TextStyle.DROP_SHADOW)
    }

    fun renderItem(context: GuiGraphics, item: ItemStack, x: Float, y: Float, scale: Float) {
        //#if MC >= 1.21.7
        //$$ context.pose().pushMatrix()
        //$$ context.pose().translate(x, y)
        //$$ context.pose().scale(scale, scale)
        //#else
        context.pose().pushPose()
        context.pose().translate(x, y, 0f)
        context.pose().scale(scale, scale, 1f)
        //#endif

        context.renderItem(item, 0, 0)

        //#if MC >= 1.21.7
        //$$ context.pose().popMatrix()
        //#else
        context.pose().popPose()
        //#endif
    }

    private val textureCache = mutableMapOf<UUID, PlayerSkin>()
    private var lastCacheClear = System.currentTimeMillis()

    fun drawPlayerHead(context: GuiGraphics, x: Int, y: Int, size: Int, uuid: UUID) {
        val now = System.currentTimeMillis()
        if (now - lastCacheClear > 300000L) {
            textureCache.clear()
            lastCacheClear = now
        }

        val textures = textureCache.getOrElse(uuid) {
            //#if MC >= 1.21.9
            //$$ val profile = client.connection?.getPlayerInfo(uuid)?.profile
            //$$ val skin = if (profile != null) {
            //$$     client.skinManager
            //$$         .get(profile)
            //$$         .getNow(Optional.empty())
            //$$         .orElseGet { DefaultPlayerSkin.get(uuid) }
            //$$ } else {
            //$$     DefaultPlayerSkin.get(uuid)
            //$$ }
            //#else
            val skin = SkullBlockEntity.fetchGameProfile(uuid)
                .getNow(Optional.empty())
                .map(client.skinManager::getInsecureSkin)
                .orElseGet { DefaultPlayerSkin.get(uuid) }
            //#endif

            val defaultSkin = DefaultPlayerSkin.get(uuid)
            if (skin.texture() != defaultSkin.texture()) textureCache[uuid] = skin
            skin
        }

        PlayerFaceRenderer.draw(context, textures, x, y, size)
    }

    fun drawRect(ctx: GuiGraphics, x: Int, y: Int, width: Int, height: Int, color: Color = Color.WHITE) {
        //#if MC >= 1.21.7
        //$$ ctx.fill(RenderPipelines.GUI, x, y, x + width, y + height, color.rgb)
        //#else
        ctx.fill(RenderType.gui(), x, y, x + width, y + height, color.rgb)
        //#endif
    }

    inline fun GuiGraphics.pushPop(block: () -> Unit) {
        //#if MC >= 1.21.7
        //$$ pose().pushMatrix()
        //#else
        pose().pushPose()
        //#endif
        block()
        //#if MC >= 1.21.7
        //$$ pose().popMatrix()
        //#else
        pose().popPose()
        //#endif
    }

    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { client.font.width(it.stripColor()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return client.font.lineHeight * lineCount
    }
}