package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.item.component.CustomData
import org.lwjgl.glfw.GLFW
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.MouseEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import java.awt.Color

@Module
object DungeonWaypoints : Feature(
    "dungeonWaypoints",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    val overrideColors by ConfigDelegate<Boolean>("dungeonWaypoints.overrideColors")
    val overrideOnSave by ConfigDelegate<Boolean>("dungeonWaypoints.overrideOnSave")

    val startColor by ConfigDelegate<Color>("dungeonWaypoints.startColor")
    val mineColor by ConfigDelegate<Color>("dungeonWaypoints.mineColor")
    val superBoomColor by ConfigDelegate<Color>("dungeonWaypoints.superboomColor")
    val etherWarpColor by ConfigDelegate<Color>("dungeonWaypoints.etherwarpColor")
    val secretColor by ConfigDelegate<Color>("dungeonWaypoints.secretColor")
    val batColor by ConfigDelegate<Color>("dungeonWaypoints.batColor")
    val leverColor by ConfigDelegate<Color>("dungeonWaypoints.leverColor")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Dungeon waypoints",
                "Shows waypoints for secrets in dungeon rooms",
                "General",
                ConfigElement(
                    "dungeonWaypoints",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Render text",
                ConfigElement(
                    "dungeonWaypoints.renderText",
                    ElementType.Switch(true)
                )
            )
            .addFeatureOption(
                "Text scale",
                ConfigElement(
                    "dungeonWaypoints.textScale",
                    ElementType.Slider(0.1, 4.0, 1.0, false)
                )
            )
            .addFeatureOption(
                "Override colors",
                ConfigElement(
                    "dungeonWaypoints.overrideColors",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Override on save",
                ConfigElement(
                    "dungeonWaypoints.overrideOnSave",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Start color",
                ConfigElement(
                    "dungeonWaypoints.startColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 255))
                )
            )
            .addFeatureOption(
                "Mine color",
                ConfigElement(
                    "dungeonWaypoints.mineColor",
                    ElementType.ColorPicker(Color(139, 69, 19, 255))
                )
            )
            .addFeatureOption(
                "Superboom color",
                ConfigElement(
                    "dungeonWaypoints.superboomColor",
                    ElementType.ColorPicker(Color(255, 0, 0, 255))
                )
            )
            .addFeatureOption(
                "Etherwarp color",
                ConfigElement(
                    "dungeonWaypoints.etherwarpColor",
                    ElementType.ColorPicker(Color(147, 51, 234, 255))
                )
            )
            .addFeatureOption(
                "Secret color",
                ConfigElement(
                    "dungeonWaypoints.secretColor",
                    ElementType.ColorPicker(Color(255, 215, 0, 255))
                )
            )
            .addFeatureOption(
                "Bat color",
                ConfigElement(
                    "dungeonWaypoints.batColor",
                    ElementType.ColorPicker(Color(255, 105, 180, 255))
                )
            )
            .addFeatureOption(
                "Lever color",
                ConfigElement(
                    "dungeonWaypoints.leverColor",
                    ElementType.ColorPicker(Color(0, 191, 255, 255))
                )
            )
            .addFeatureOption(
                "Start Recording",
                ConfigElement(
                    "dungeonWaypoints.startRecording",
                    ElementType.Button("Start Recording") { RouteRecorder.startRecording() }
                )
            )
            .addFeatureOption(
                "Stop Recording",
                ConfigElement(
                    "dungeonWaypoints.stopRecording",
                    ElementType.Button("Stop Recording") { RouteRecorder.stopRecording() }
                )
            )
            .addFeatureOption(
                "Reload Routes (Local)",
                ConfigElement(
                    "dungeonWaypoints.reloadLocal",
                    ElementType.Button("Reload from Local") {
                        WaypointRegistry.reloadFromLocal(notifyUser = true)
                    }
                )
            )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            RoomWaypointHandler.loadWaypointsForRoom(event.new)
        }

        register<RenderEvent.World.Last> { event ->
            if (DungeonAPI.inBoss) return@register
            WaypointRenderer.render(event)
        }

        register<DungeonEvent.Secrets.Bat> { event ->
            val room = DungeonAPI.currentRoom ?: return@register
            val waypoints = RoomWaypointHandler.getWaypoints(room) ?: return@register

            SecretHandler.clickSecret(waypoints, event.entity.blockPosition(), 0)
        }

        register<DungeonEvent.Secrets.Item> { event ->
            val room = DungeonAPI.currentRoom ?: return@register
            val waypoints = RoomWaypointHandler.getWaypoints(room) ?: return@register
            val pos = KnitClient.world?.getEntity(event.entityId)?.blockPosition() ?: return@register

            SecretHandler.clickSecret(waypoints, pos, 3)
        }

        register<DungeonEvent.Secrets.Chest> { event ->
            val room = DungeonAPI.currentRoom ?: return@register
            val waypoints = RoomWaypointHandler.getWaypoints(room) ?: return@register

            SecretHandler.clickSecret(waypoints, event.blockPos, 0)
        }

        register<DungeonEvent.Secrets.Essence> { event ->
            val room = DungeonAPI.currentRoom ?: return@register
            val waypoints = RoomWaypointHandler.getWaypoints(room) ?: return@register

            SecretHandler.clickSecret(waypoints, event.blockPos, 0)
        }

        register<DungeonEvent.Secrets.Misc> { event ->
            val room = DungeonAPI.currentRoom ?: return@register
            val waypoints = RoomWaypointHandler.getWaypoints(room) ?: return@register

            SecretHandler.clickSecret(waypoints, event.blockPos, 0)
        }

        register<PacketEvent.Received> { event ->
            if (event.packet is ClientboundPlayerPositionPacket) {
                SecretHandler.handleEtherwarp(event.packet)
            }
        }

        register<LocationEvent.WorldChange> {
            RoomWaypointHandler.clear()
            SecretHandler.reset()
        }

        register<MouseEvent.Click> { event ->
            RouteRecorder.handleRightClick(event)
            RouteRecorder.handleLeftClick(event)
        }

        register<MouseEvent.Click> { event ->
            if (event.button != GLFW.GLFW_MOUSE_BUTTON_RIGHT || client.screen != null) return@register
            val item = client.player?.mainHandItem ?: return@register

            item
                .getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag()
                .takeIf {
                    it?.getInt("ethermerge")?.orElse(0) == 1 ||
                    item.getData(DataTypes.SKYBLOCK_ID)?.skyblockId == "ETHERWARP_CONDUIT"
                }
                ?.let { item ->
                    val distance = 56.0 + item.getInt("tuned_transmission").orElse(0)
                    EtherWarpHelper.getEtherPos(client.player?.position(), distance)
                        .takeIf { it.succeeded && it.pos != null }
                        ?.also {
                            SecretHandler.lastEtherTime = System.currentTimeMillis()
                            SecretHandler.lastEtherPos = it.pos
                        }
            }
        }
    }
}