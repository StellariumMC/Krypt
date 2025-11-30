package xyz.meowing.krypt.features.waypoints

data class WaypointFile(
    val rooms: Map<String, List<WaypointData>>
)