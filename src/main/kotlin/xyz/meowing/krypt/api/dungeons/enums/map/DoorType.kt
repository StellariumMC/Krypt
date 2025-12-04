package xyz.meowing.krypt.api.dungeons.enums.map

import xyz.meowing.krypt.features.map.DungeonMap
import java.awt.Color

enum class DoorType {
    BLOOD,
    ENTRANCE,
    WITHER,
    NORMAL
    ;

    val color: Color
        get() = when (this) {
            BLOOD -> DungeonMap.bloodDoorColor
            ENTRANCE -> DungeonMap.entranceDoorColor
            WITHER -> DungeonMap.witherDoorColor
            NORMAL -> DungeonMap.normalDoorColor
        }

    companion object {
        fun fromMapColor(color: Int): DoorType? = when (color) {
            18 -> BLOOD
            30 -> ENTRANCE
            // Champion, Fairy, Puzzle, Trap, Unopened doors render as normal doors
            74, 82, 66, 62, 85, 63 -> NORMAL
            119 -> WITHER
            else -> null
        }
    }
}