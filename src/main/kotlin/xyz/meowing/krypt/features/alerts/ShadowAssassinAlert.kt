package xyz.meowing.krypt.features.alerts

import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils.showTitle

@Module
object ShadowAssassinAlert : Feature(
    "shadowAssassinAlert",
    "Shadow assassin alert",
    "Alerts you when a shadow assassin is about to teleport",
    "Alerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun initialize() {
        register<PacketEvent.Received> { event ->
            val packet = event.packet as? ClientboundInitializeBorderPacket ?: return@register
            if (packet.newSize != 1.0) return@register

            showTitle(subtitle = if (DungeonAPI.floor?.floorNumber == 1) "§cBonzo Respawn" else "§8Shadow Assassin", duration = 2000)
        }
    }
}