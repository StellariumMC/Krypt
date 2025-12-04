package xyz.meowing.krypt.api.dungeons.enums

import xyz.meowing.krypt.features.map.DungeonMap
import java.awt.Color

enum class DungeonClass(val displayName: String) {
    UNKNOWN("Unknown"),
    HEALER("Healer"),
    MAGE("Mage"),
    BERSERK("Berserk"),
    ARCHER("Archer"),
    TANK("Tank"),
    DEAD("DEAD")
    ;

    val mapColor: Color
        get() = when (this) {
            HEALER -> DungeonMap.healerColor
            MAGE -> DungeonMap.mageColor
            BERSERK -> DungeonMap.berserkColor
            ARCHER -> DungeonMap.archerColor
            TANK -> DungeonMap.tankColor
            else -> defaultColor
        }

    companion object {
        private val classes: Map<String, DungeonClass> = entries.toTypedArray().associateBy { it.displayName }
        val defaultColor = Color(0, 0, 0, 255)
        fun from(name: String): DungeonClass = classes[name] ?: UNKNOWN
    }
}