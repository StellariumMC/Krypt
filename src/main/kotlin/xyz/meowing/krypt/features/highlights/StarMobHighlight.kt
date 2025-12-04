package xyz.meowing.krypt.features.highlights

import net.minecraft.world.entity.decoration.ArmorStand
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.glowThisFrame
import xyz.meowing.krypt.utils.glowingColor

@Module
object StarMobHighlight : Feature(
    "starMobHighlight",
    "Star mob highlight",
    "Highlights starred mobs in dungeons",
    "Highlights",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val entities = mutableListOf<Int>()
    private val starMobsColor by config.colorPicker("Color")

    override fun initialize() {
        register<EntityEvent.Join> { event ->
            if (event.entity !is ArmorStand) return@register
            val entity = event.entity

            TickScheduler.Server.schedule(2) {
                val name = entity.name.string
                if (!name.contains("✯ ")) return@schedule

                val offset = if (name.contains("Withermancer")) 3 else 1
                entities.add(entity.id - offset)
            }
        }

        register<LocationEvent.WorldChange> {
            entities.clear()
        }

        register<RenderEvent.Entity.Pre> { event ->
            val entity = event.entity
            if (!entities.contains(entity.id)) return@register

            if (player?.hasLineOfSight(entity) == true) {
                entity.glowThisFrame = true
                entity.glowingColor = starMobsColor.rgb
            }
        }
    }
}