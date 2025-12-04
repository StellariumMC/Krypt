package xyz.meowing.krypt.features.highlights

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.features.Feature
import java.awt.Color

@Module
object TeammateHighlight : Feature(
    "teammateHighlight",
    "Teammate highlight",
    "Highlights teammates in dungeons",
    "Highlights",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val mageColor by config.colorPicker("Mage color", Color(85, 255, 255))
    private val archerColor by config.colorPicker("Archer color", Color(255, 170, 0))
    private val healerColor by config.colorPicker("Healer color", Color(255, 85, 255))
    private val tankColor by config.colorPicker("Tank color", Color(0, 170, 0))
    private val bersColor by config.colorPicker("Berserk color", Color(170, 0, 0))

    @JvmStatic
    fun getTeammateColor(entity: Entity): Int? {
        if (!isEnabled()) return null
        if (entity !is Player) return null
        return DungeonAPI.players.find { it?.name == entity.name.stripped }?.dungeonClass?.getColor()?.rgb
    }

    private fun DungeonClass?.getColor(): Color? {
        return when (this) {
            DungeonClass.MAGE -> mageColor
            DungeonClass.ARCHER -> archerColor
            DungeonClass.HEALER -> healerColor
            DungeonClass.TANK -> tankColor
            DungeonClass.BERSERK -> bersColor
            else -> null
        }
    }
}