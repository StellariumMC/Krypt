package xyz.meowing.krypt.features.highlights

import net.minecraft.world.entity.boss.wither.WitherBoss
import xyz.meowing.knit.api.KnitClient.player
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.glowThisFrame
import xyz.meowing.krypt.utils.glowingColor
import java.awt.Color

@Module
object WitherHighlight : Feature(
    "witherHighlight",
    "Wither highlight",
    "Highlights withers in F7/M7",
    "Highlights",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val color by config.colorPicker("Color", Color(0, 255, 255, 127))

    override fun initialize() {
        register<RenderEvent.Entity.Pre> { event ->
            val entity = event.entity as? WitherBoss ?: return@register
            if (entity.isInvisible || entity.invulnerableTicks == 800) return@register
            if (player?.hasLineOfSight(entity) == false) return@register

            entity.glowThisFrame = true
            entity.glowingColor = color.rgb
        }
    }
}