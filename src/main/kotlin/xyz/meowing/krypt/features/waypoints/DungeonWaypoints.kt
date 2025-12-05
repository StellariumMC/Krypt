package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.item.component.CustomData
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.datatype.getData
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.input.KnitMouseButtons
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.MouseEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import java.awt.Color

@Module
object DungeonWaypoints : Feature(
    "dungeonWaypoints",
    "Dungeon waypoints",
    "Shows waypoints for secrets in dungeon rooms",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    val overrideColors by config.switch("Override colors")
    val overrideOnSave by config.switch("Override on save")

    val onlyRenderAfterClear by config.switch("Only render after clear", true)
    val stopRenderAfterGreen by config.switch("Stop render after green", true)
    val renderText by config.switch("Render text", true)
    val textRenderDistance by config.slider("Text render distance", 10.0, 1.0, 20.0, false)
    val textScale by config.slider("Text scale", 1.0, 0.1, 4.0, true)

    val startColor by config.colorPicker("Start color", Color(0, 255, 0, 255))
    val mineColor by config.colorPicker("Mine color", Color(139, 69, 19, 255))
    val superBoomColor by config.colorPicker("Superboom color", Color(255, 0, 0, 255))
    val etherWarpColor by config.colorPicker("Etherwarp color", Color(147, 51, 234, 255))
    val secretColor by config.colorPicker("Secret color", Color(255, 215, 0, 255))
    val batColor by config.colorPicker("Bat color", Color(255, 105, 180, 255))
    val leverColor by config.colorPicker("Lever color", Color(0, 191, 255, 255))

    init {
        config.button("Start Recording") {
            RouteRecorder.startRecording()
        }

        config.button("Stop Recording") {
            RouteRecorder.stopRecording()
        }

        config.button("Reload Routes") {
            WaypointRegistry.reloadFromLocal(notifyUser = true)
        }
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
            if (event.button != KnitMouseButtons.RIGHT.code || client.screen != null) return@register
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