package xyz.meowing.krypt.features.waypoints

import java.awt.Color

enum class WaypointType {
    START,
    SECRET,
    BAT,
    MINE,
    LEVER,
    SUPERBOOM,
    ETHERWARP
    ;

    val color: Color
        get() = when (this) {
            BAT -> DungeonWaypoints.batColor
            MINE -> DungeonWaypoints.mineColor
            SECRET -> DungeonWaypoints.secretColor
            ETHERWARP -> DungeonWaypoints.etherWarpColor
            SUPERBOOM -> DungeonWaypoints.superBoomColor
            LEVER -> DungeonWaypoints.leverColor
            START -> DungeonWaypoints.startColor
        }

    companion object {
        fun fromString(value: String?): WaypointType? {
            if (value == null) return null
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}