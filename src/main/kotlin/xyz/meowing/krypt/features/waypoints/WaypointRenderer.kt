package xyz.meowing.krypt.features.waypoints

import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.utils.rendering.Render3D

object WaypointRenderer {
    private val renderText by ConfigDelegate<Boolean>("dungeonWaypoints.renderText")
    private val textScale by ConfigDelegate<Double>("dungeonWaypoints.textScale")

    fun render(event: RenderEvent.World.Last) {
        val room = DungeonAPI.currentRoom ?: return
        val waypoints = RoomWaypointHandler.getWaypoints(room) ?: return

        val matrices = event.context.matrixStack()
        val consumers = event.context.consumers()

        waypoints.forEach { waypoint ->
            if (waypoint.clicked) return@forEach
            val color = if (DungeonWaypoints.overrideColors) (waypoint.type?.color ?: WaypointType.MINE.color) else waypoint.color
            val block = waypoint.aabb.move(waypoint.blockPos)

            val style = when {
                waypoint.filled && waypoint.depth -> 2
                waypoint.filled -> 1
                else -> 0
            }

            when (style) {
                0 -> {
                    Render3D.drawOutlinedBB(
                        block,
                        color,
                        consumers,
                        matrices,
                        true
                    )
                }

                1 -> {
                    Render3D.drawFilledBB(
                        block,
                        color,
                        consumers,
                        matrices,
                        true
                    )
                }

                2 -> {
                    Render3D.drawSpecialBB(
                        block,
                        color,
                        consumers,
                        matrices,
                        true
                    )
                }
            }

            if (renderText) {
                val title = waypoint.title ?: if (waypoint.type == WaypointType.START) "Start" else return@forEach
                val center = block.center.add(0.0, 0.1 * textScale, 0.0)

                Render3D.drawString(
                    title,
                    center,
                    matrices,
                    scale = textScale.toFloat(),
                    depth = false
                )
            }
        }
    }
}