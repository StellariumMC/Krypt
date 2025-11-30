package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.BlockPos
import java.awt.Color

data class WaypointData(
    val x: Int,
    val y: Int,
    val z: Int,
    val color: String,
    val filled: Boolean,
    val depth: Boolean,
    val aabb: AABBData,
    val title: String? = null,
    val type: String? = null
) {
    fun toWaypoint(blockPos: BlockPos): Waypoint {
        val waypointColor = Color.decode(color.replace("FF", ""))
        val alpha = color.takeLast(2).toInt(16)
        val finalColor = Color(waypointColor.red, waypointColor.green, waypointColor.blue, alpha)

        return Waypoint(
            blockPos = blockPos,
            color = finalColor,
            filled = filled,
            depth = depth,
            aabb = aabb.toAABB(),
            title = title,
            type = WaypointType.fromString(type)
        )
    }
}