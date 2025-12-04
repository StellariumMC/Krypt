package xyz.meowing.krypt.features.alerts

import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.decoration.ArmorStand
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.EntityEvent

@Module
object BatDeathAlert : Feature(
    "batDeathAlert",
    "Bat death alert",
    "Shows a title when bats die in dungeons",
    "Alerts",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    override fun initialize() {
        register<EntityEvent.Death> {
            if (it.entity is Bat && it.entity.vehicle !is ArmorStand && !DungeonAPI.inBoss) {
                TitleUtils.showTitle("§cBat Dead!", null, 1000)
            }
        }
    }
}