package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import java.awt.Color

data class Waypoint(
    val blockPos: BlockPos,
    val color: Color,
    val filled: Boolean,
    val depth: Boolean,
    val aabb: AABB,
    val title: String? = null,
    val type: WaypointType? = null,
    var clicked: Boolean = false
) {
    val isSecret: Boolean
        get() = type == WaypointType.SECRET || type == WaypointType.BAT || type == WaypointType.LEVER

    val isEtherwarp: Boolean
        get() = type == WaypointType.ETHERWARP
}