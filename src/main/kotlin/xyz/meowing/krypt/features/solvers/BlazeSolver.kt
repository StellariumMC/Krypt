package xyz.meowing.krypt.features.solvers

import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.monster.Blaze
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.knit.api.utils.StringUtils.remove
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.ClientTick
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.solvers.data.PuzzleTimer
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.glowThisFrame
import xyz.meowing.krypt.utils.glowingColor
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

/**
 * Contains modified code from Noamm's blaze solver.
 *
 * Original File: [GitHub](https://github.com/Noamm9/NoammAddons/blob/master/src/main/kotlin/noammaddons/features/impl/dungeons/solvers/puzzles/BlazeSolver.kt)
 */
@Module
object BlazeSolver : Feature(
    "blazeSolver",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val blazeHpRegex = Regex("^\\[Lv15].+Blaze [\\d,]+/([\\d,]+)❤$")

    private var inBlaze = false
    private val blazes = mutableListOf<Blaze>()
    private var lastBlazeCount = 10
    private val hpMap = mutableMapOf<Blaze, Int>()
    private var reversed = false
    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null

    private val blazeCount by ConfigDelegate<Double>("blazeSolver.count")
    private val lineColor by ConfigDelegate<Color>("blazeSolver.lineColor")
    private val firstBlazeColor by ConfigDelegate<Color>("blazeSolver.firstColor")
    private val secondBlazeColor by ConfigDelegate<Color>("blazeSolver.secondColor")
    private val thirdBlazeColor by ConfigDelegate<Color>("blazeSolver.thirdColor")

    override fun addConfig() {
        ConfigManager
            .addFeature(
                "Blaze solver",
                "Highlights blazes in order and tracks completion time",
                "Solvers",
                ConfigElement(
                    "blazeSolver",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Number of blazes to show",
                ConfigElement(
                    "blazeSolver.count",
                    ElementType.Slider(1.0, 10.0, 3.0, false)
                )
            )
            .addFeatureOption(
                "Line color",
                ConfigElement(
                    "blazeSolver.lineColor",
                    ElementType.ColorPicker(Color(255, 255, 255, 255))
                )
            )
            .addFeatureOption(
                "First blaze color",
                ConfigElement(
                    "blazeSolver.firstColor",
                    ElementType.ColorPicker(Color(0, 255, 0, 127))
                )
            )
            .addFeatureOption(
                "Second blaze color",
                ConfigElement(
                    "blazeSolver.secondColor",
                    ElementType.ColorPicker(Color(255, 255, 0, 127))
                )
            )
            .addFeatureOption(
                "Third blaze color",
                ConfigElement(
                    "blazeSolver.thirdColor",
                    ElementType.ColorPicker(Color(255, 165, 0, 127))
                )
            )
    }

    override fun initialize() {
        setupLoops {
            loop<ClientTick>(5) {
                if (!inBlaze) return@loop
                solveBlaze()
            }
        }

        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name?.contains("Blaze", true) == false) return@register

            inBlaze = true
            reversed = event.new.name?.equals("Lower Blaze", true) == true
            trueTimeStarted = System.currentTimeMillis()
            lastBlazeCount = 10
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inBlaze && event.new.name?.contains("Blaze", true) == false) reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<RenderEvent.World.Last> { event ->
            if (blazes.isEmpty()) return@register

            blazes.withIndex().forEach { (i, entity) ->
                if (i > 0 && i < blazeCount.toInt()) {
                    val b1 = blazes[i - 1].position().add(0.0, blazes[i - 1].bbHeight / 2.0, 0.0)
                    val b2 = entity.position().add(0.0, entity.bbHeight / 2.0, 0.0)
                    Render3D.drawLine(b1, b2, 1f, lineColor, event.context.consumers(), event.context.matrixStack())
                }
            }
        }

        register<RenderEvent.Entity.Pre> { event ->
            if (blazes.isEmpty()) return@register
            if (player?.hasLineOfSight(event.entity) == false) return@register

            val index = blazes.indexOf(event.entity as? Blaze)
            if (index != -1 && index < blazeCount.toInt()) {
                event.entity.glowThisFrame = true
                event.entity.glowingColor = getBlazeColor(index).rgb
            }
        }
    }

    private fun solveBlaze() {
        blazes.clear()
        hpMap.clear()

        val world = KnitClient.world ?: return

        world.entitiesForRendering().filterIsInstance<ArmorStand>().forEach { armorStand ->
            val match = blazeHpRegex.find(armorStand.name.stripped) ?: return@forEach
            val health = match.groupValues[1].remove(",").toIntOrNull() ?: return@forEach

            val nearbyEntities = world.getEntities(
                armorStand,
                armorStand.boundingBox.move(0.0, -1.0, 0.0)
            )

            val blaze = nearbyEntities.filterIsInstance<Blaze>().firstOrNull() ?: return@forEach

            if (blazes.contains(blaze) || hpMap.keys.contains(blaze)) return@forEach
            hpMap[blaze] = health
            blazes.add(blaze)
        }

        blazes.sortWith(Comparator.comparingInt { hpMap[it]!! })
        if (blazes.isNotEmpty() && reversed) blazes.reverse()

        if (blazes.size == 10 && trueTimeStarted == null) trueTimeStarted = System.currentTimeMillis()
        if (blazes.size == 9 && timeStarted == null) timeStarted = System.currentTimeMillis()

        if (blazes.isEmpty() && lastBlazeCount == 1) {
            val trueTime = trueTimeStarted ?: return
            val startTime = timeStarted ?: return

            val solveTime = (System.currentTimeMillis() - startTime).toDouble()
            val totalTime = (System.currentTimeMillis() - trueTime).toDouble()

            PuzzleTimer.submitTime("Blaze", solveTime, totalTime)

            trueTimeStarted = null
            timeStarted = null
        }

        lastBlazeCount = blazes.size
    }

    private fun getBlazeColor(index: Int) = when (index) {
        0 -> firstBlazeColor
        1 -> secondBlazeColor
        else -> thirdBlazeColor
    }

    private fun reset() {
        inBlaze = false
        reversed = false
        blazes.clear()
        hpMap.clear()
        trueTimeStarted = null
        timeStarted = null
        lastBlazeCount = 10
    }
}