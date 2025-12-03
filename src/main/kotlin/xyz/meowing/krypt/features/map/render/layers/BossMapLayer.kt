package xyz.meowing.krypt.features.map.render.layers

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import tech.thatgravyboat.skyblockapi.platform.drawTexture
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import kotlin.math.min

object BossMapLayer {
    private const val MAP_SIZE = 128
    private const val TEXTURE_SIZE = 256.0

    fun render(context: GuiGraphics, bossMap: BossMapData) {
        val playerPos = KnitPlayer.player?.position() ?: return

        val scale = calculateScale(bossMap)
        val (offsetX, offsetY) = calculateOffsets(bossMap, playerPos, scale)

        context.enableScissor(5, 5, MAP_SIZE + 10, MAP_SIZE + 10)

        renderMapImage(context, bossMap.image, offsetX, offsetY, scale)
        renderPlayers(context, bossMap, offsetX, offsetY)

        context.disableScissor()
    }

    private fun renderPlayers(context: GuiGraphics, bossMap: BossMapData, offsetX: Double, offsetY: Double) {
        DungeonAPI.players.filterNotNull().forEach { player ->
            if (player.dead && player.name != KnitPlayer.name) return@forEach

            val playerEntity = player.entity ?: return@forEach
            val sizeInWorld = min(bossMap.widthInWorld, bossMap.heightInWorld)

            val x = ((playerEntity.x - bossMap.topLeftLocation[0]) / sizeInWorld) * MAP_SIZE - offsetX
            val y = ((playerEntity.z - bossMap.topLeftLocation[1]) / sizeInWorld) * MAP_SIZE - offsetY

            player.iconX = x
            player.iconZ = y
        }

        PlayerLayer.render(context)

        DungeonAPI.players.filterNotNull().forEach { player ->
            if (player.iconX != null && player.iconZ != null) {
                player.iconX = player.iconX!! / 128.0 * 125.0
                player.iconZ = player.iconZ!! / 128.0 * 125.0
            }
        }
    }

    private fun calculateScale(bossMap: BossMapData): Double {
        val renderSize = bossMap.renderSize ?: bossMap.widthInWorld
        return MAP_SIZE / (TEXTURE_SIZE / bossMap.widthInWorld * renderSize)
    }

    private fun calculateOffsets(bossMap: BossMapData, playerPos: net.minecraft.world.phys.Vec3, scale: Double): Pair<Double, Double> {
        val sizeInWorld = min(bossMap.widthInWorld, bossMap.heightInWorld)

        var offsetX = ((playerPos.x - bossMap.topLeftLocation[0]) / sizeInWorld) * MAP_SIZE - MAP_SIZE / 2
        var offsetY = ((playerPos.z - bossMap.topLeftLocation[1]) / sizeInWorld) * MAP_SIZE - MAP_SIZE / 2

        offsetX = offsetX.coerceIn(0.0, maxOf(0.0, TEXTURE_SIZE * scale - MAP_SIZE))
        offsetY = offsetY.coerceIn(0.0, maxOf(0.0, TEXTURE_SIZE * scale - MAP_SIZE))

        return offsetX to offsetY
    }

    private fun renderMapImage(context: GuiGraphics, image: String, offsetX: Double, offsetY: Double, scale: Double) {
        val texture = ResourceLocation.fromNamespaceAndPath(Krypt.NAMESPACE, "dungeons/map/boss/$image.png")
        context.drawTexture(
            texture,
            (-offsetX + 5).toInt(),
            (-offsetY + 5).toInt(),
            (TEXTURE_SIZE * scale).toInt(),
            (TEXTURE_SIZE * scale).toInt()
        )
    }

    data class BossMapData(
        val image: String,
        val bounds: List<List<Double>>,
        val widthInWorld: Int,
        val heightInWorld: Int,
        val topLeftLocation: List<Int>,
        val renderSize: Int? = null
    )
}