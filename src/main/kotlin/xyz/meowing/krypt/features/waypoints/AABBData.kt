package xyz.meowing.krypt.features.waypoints

import net.minecraft.world.phys.AABB

data class AABBData(
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double
) {
    fun toAABB(): AABB = AABB(minX, minY, minZ, maxX, maxY, maxZ)
}
