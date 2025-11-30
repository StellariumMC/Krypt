package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.phys.Vec3
import xyz.meowing.krypt.api.dungeons.DungeonAPI

object SecretHandler {
    var lastEtherPos: BlockPos? = null
    var lastEtherTime = 0L

    fun handleEtherwarp(packet: ClientboundPlayerPositionPacket) {
        val room = DungeonAPI.currentRoom ?: return
        val etherPos = lastEtherPos ?: return
        val waypoints = RoomWaypointHandler.getWaypoints(room) ?: return

        if (System.currentTimeMillis() - lastEtherTime > 1000) return
        if (packet.change.position.distanceTo(Vec3.atCenterOf(etherPos)) > 3) return

        waypoints.find { it.blockPos == etherPos && it.isEtherwarp }?.let {
            it.clicked = true
            lastEtherPos = null
            lastEtherTime = 0L
        }
    }

    fun reset() {
        lastEtherPos = null
        lastEtherTime = 0L
    }

    fun clickSecret(waypoints: List<Waypoint>, pos: BlockPos, distance: Int) {
        val waypoint = if (distance == 0) {
            waypoints.find { it.blockPos == pos && it.isSecret && !it.clicked }
        } else {
            waypoints
                .filter { it.isSecret && !it.clicked }
                .minByOrNull { it.blockPos.distSqr(pos) }
                ?.takeIf { it.blockPos.distSqr(pos) <= distance }
        }

        waypoint?.clicked = true
    }
}