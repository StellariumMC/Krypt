package xyz.meowing.krypt.features.highlights

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.Utils.toFloatArray
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object KeyHighlight : Feature(
    "keyHighlight",
    "Key highlight",
    "Highlight blood/wither door key in the world",
    "Highlights",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val filled by config.switch("Filled box")
    private val outlined by config.switch("Outlined box", true)
    private val highlightWither by config.switch("Highlight wither key", true)
    private val highlightBlood by config.switch("Highlight blood key", true)
    private val witherColor by config.colorPicker("Wither key color", Color(0, 0, 0, 255))
    private val bloodColor by config.colorPicker("Blood key color", Color(255, 0, 0, 255))

    private var doorKey: Pair<Entity, Color>? = null

    override fun initialize() {
        register<EntityEvent.Packet.Metadata> { event ->
            if (DungeonAPI.inBoss) return@register

            val entityName = event.entity.name.stripped

            when (entityName) {
                "Wither Key" -> if (highlightWither) doorKey = Pair(event.entity, witherColor)
                "Blood Key" -> if (highlightBlood) doorKey = Pair(event.entity, bloodColor)
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (DungeonAPI.inBoss) return@register

            doorKey?.let { (entity, color) ->
                if (entity.isRemoved) {
                    doorKey = null
                    return@register
                }

                val matrixStack = event.context.matrixStack()
                val consumers = event.context.consumers()

                Render3D.drawLineToEntity(
                    entity,
                    consumers,
                    matrixStack,
                    color.toFloatArray(),
                    1f
                )

                val box = AABB(
                    entity.x - 0.4,
                    entity.y + 1.2,
                    entity.z - 0.4,
                    entity.x + 0.4,
                    entity.y + 2.0,
                    entity.z + 0.4
                )

                if (filled) {
                    Render3D.drawFilledBB(
                        box,
                        color,
                        consumers,
                        matrixStack
                    )
                }

                if (outlined) {
                    Render3D.drawOutlinedBB(
                        box,
                        color.darker(),
                        consumers,
                        matrixStack
                    )
                }
            }
        }

        register<LocationEvent.WorldChange> {
            doorKey = null
        }
    }
}