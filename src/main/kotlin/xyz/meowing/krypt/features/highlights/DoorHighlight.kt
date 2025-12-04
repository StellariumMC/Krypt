package xyz.meowing.krypt.features.highlights

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.utils.regex.matchWhen
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonKey
import xyz.meowing.krypt.api.dungeons.enums.map.DoorState
import xyz.meowing.krypt.api.dungeons.enums.map.DoorType
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object DoorHighlight : Feature(
    "doorHighlight",
    "Door highlight",
    "Highlight wither/blood doors through walls",
    "Highlights",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val keyObtainedRegex = Regex("(?:\\[.+] ?)?\\w+ has obtained (?<type>\\w+) Key!")
    private val keyPickedUpRegex = Regex("A (?<type>\\w+) Key was picked up!")
    private val witherDoorOpenRegex = Regex("\\w+ opened a WITHER door!")
    private val bloodDoorOpenRegex = Regex("The BLOOD DOOR has been opened!")

    private var witherKeyObtained = false
    private var bloodKeyObtained = false
    private var bloodOpen = false

    private val filled by config.switch("Filled box")
    private val outlined by config.switch("Outlined box", true)
    private val witherWithKey by config.colorPicker("Wither door with key", Color(0, 255, 0, 255))
    private val witherNoKey by config.colorPicker("Wither door without key", Color(255, 255, 255, 255))
    private val bloodWithKey by config.colorPicker("Blood door with key", Color(0, 255, 0, 255))
    private val bloodNoKey by config.colorPicker("Blood dor without key", Color(255, 0, 0, 255))

    override fun initialize() {
        register<LocationEvent.WorldChange> { reset() }

        register<ChatEvent.Receive> { event ->
            val message = event.message.stripped

            matchWhen(message) {
                case(keyObtainedRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(keyPickedUpRegex, "type") { (type) ->
                    handleGetKey(type)
                }

                case(witherDoorOpenRegex) {
                    witherKeyObtained = false
                }

                case(bloodDoorOpenRegex) {
                    bloodKeyObtained = false
                    bloodOpen = true
                }
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (bloodOpen) return@register

            DungeonAPI.doors.forEach { door ->
                if (door == null) return@forEach
                if (door.state != DoorState.DISCOVERED) return@forEach
                if (door.opened && !door.isFairyDoor) return@forEach
                if (door.type !in setOf(DoorType.WITHER, DoorType.BLOOD) && !door.isFairyDoor) return@forEach

                val color = when {
                    door.isFairyDoor -> if (witherKeyObtained) witherWithKey else witherNoKey
                    door.type == DoorType.WITHER -> if (witherKeyObtained) witherWithKey else witherNoKey
                    door.type == DoorType.BLOOD -> if (bloodKeyObtained) bloodWithKey else bloodNoKey
                    else -> return@forEach
                }

                val (x, y, z) = door.getPos()

                val block = KnitClient.world?.getBlockState(BlockPos(x, y + 1, z))

                if ((block?.block == Blocks.BARRIER || block?.isAir == true) && !door.isFairyDoor) return@forEach

                val box = AABB(
                    x.toDouble() - 1.0, y.toDouble(), z.toDouble() - 1,
                    x.toDouble() + 2.0, y.toDouble() + 4.0, z.toDouble() + 2
                )

                val matrixStack = event.context.matrixStack()
                val consumers = event.context.consumers()

                if (filled) {
                    Render3D.drawFilledBB(
                        box,
                        color,
                        consumers,
                        matrixStack,
                        phase = true
                    )
                }

                if (outlined) {
                    Render3D.drawOutlinedBB(
                        box,
                        color.darker(),
                        consumers,
                        matrixStack,
                        phase = true
                    )
                }
            }
        }
    }

    private fun handleGetKey(type: String) {
        val key = DungeonKey.getById(type) ?: return
        when (key) {
            DungeonKey.WITHER -> witherKeyObtained = true
            DungeonKey.BLOOD -> bloodKeyObtained = true
        }
    }

    private fun reset() {
        witherKeyObtained = false
        bloodKeyObtained = false
        bloodOpen = false
    }
}