package xyz.meowing.krypt.features.floor7

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonPhase
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object TerminalWaypoints : Feature(
    "terminalWaypoints",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val checkClass by ConfigDelegate<Boolean>("terminalWaypoints.focusMode")
    private val showText by ConfigDelegate<Boolean>("terminalWaypoints.renderText")
    private val highlightStyle by ConfigDelegate<Int>("terminalWaypoints.highlightStyle")
    private val terminalColor by ConfigDelegate<Color>("terminalWaypoints.terminalColor")
    private val leverColor by ConfigDelegate<Color>("terminalWaypoints.leverColor")

    private val terminal1Class by ConfigDelegate<Int>("terminalWaypoints.terminal1Class")
    private val terminal2Class by ConfigDelegate<Int>("terminalWaypoints.terminal2Class")
    private val terminal3Class by ConfigDelegate<Int>("terminalWaypoints.terminal3Class")
    private val terminal4Class by ConfigDelegate<Int>("terminalWaypoints.terminal4Class")
    private val terminal5Class by ConfigDelegate<Int>("terminalWaypoints.terminal5Class")
    private val terminal6Class by ConfigDelegate<Int>("terminalWaypoints.terminal6Class")
    private val terminal7Class by ConfigDelegate<Int>("terminalWaypoints.terminal7Class")
    private val terminal8Class by ConfigDelegate<Int>("terminalWaypoints.terminal8Class")
    private val terminal9Class by ConfigDelegate<Int>("terminalWaypoints.terminal9Class")
    private val terminal10Class by ConfigDelegate<Int>("terminalWaypoints.terminal10Class")
    private val terminal11Class by ConfigDelegate<Int>("terminalWaypoints.terminal11Class")
    private val terminal12Class by ConfigDelegate<Int>("terminalWaypoints.terminal12Class")
    private val terminal13Class by ConfigDelegate<Int>("terminalWaypoints.terminal13Class")
    private val terminal14Class by ConfigDelegate<Int>("terminalWaypoints.terminal14Class")
    private val terminal15Class by ConfigDelegate<Int>("terminalWaypoints.terminal15Class")
    private val terminal16Class by ConfigDelegate<Int>("terminalWaypoints.terminal16Class")
    private val terminal17Class by ConfigDelegate<Int>("terminalWaypoints.terminal17Class")
    private val terminal18Class by ConfigDelegate<Int>("terminalWaypoints.terminal18Class")
    private val terminal19Class by ConfigDelegate<Int>("terminalWaypoints.terminal19Class")
    private val terminal20Class by ConfigDelegate<Int>("terminalWaypoints.terminal20Class")
    private val terminal21Class by ConfigDelegate<Int>("terminalWaypoints.terminal21Class")
    private val terminal22Class by ConfigDelegate<Int>("terminalWaypoints.terminal22Class")
    private val terminal23Class by ConfigDelegate<Int>("terminalWaypoints.terminal23Class")
    private val terminal24Class by ConfigDelegate<Int>("terminalWaypoints.terminal24Class")
    private val terminal25Class by ConfigDelegate<Int>("terminalWaypoints.terminal25Class")

    private data class Terminal(
        val positions: List<BlockPos>,
        val isLever: Boolean,
        val defaultClass: DungeonClass,
        val configIndex: Int,
        val section: DungeonPhase.P3
    )

    private val classOptions = listOf(
        "Healer",
        "Mage",
        "Berserk",
        "Archer",
        "Tank"
    )

    private val classMapping = mapOf(
        0 to DungeonClass.HEALER,
        1 to DungeonClass.MAGE,
        2 to DungeonClass.BERSERK,
        3 to DungeonClass.ARCHER,
        4 to DungeonClass.TANK
    )

    private val terminals = listOf(
        Terminal(listOf(BlockPos(111, 113, 73), BlockPos(110, 113, 73)), false, DungeonClass.TANK, 1, DungeonPhase.P3.S1),
        Terminal(listOf(BlockPos(111, 119, 79), BlockPos(110, 119, 79)), false, DungeonClass.TANK, 2, DungeonPhase.P3.S1),
        Terminal(listOf(BlockPos(89, 112, 92), BlockPos(90, 112, 92)), false, DungeonClass.MAGE, 3, DungeonPhase.P3.S1),
        Terminal(listOf(BlockPos(89, 122, 101), BlockPos(90, 122, 101)), false, DungeonClass.MAGE, 4, DungeonPhase.P3.S1),
        Terminal(listOf(BlockPos(94, 124, 113), BlockPos(94, 125, 113)), true, DungeonClass.ARCHER, 5, DungeonPhase.P3.S1),
        Terminal(listOf(BlockPos(106, 124, 113), BlockPos(106, 125, 113)), true, DungeonClass.ARCHER, 6, DungeonPhase.P3.S1),

        Terminal(listOf(BlockPos(68, 109, 121), BlockPos(68, 109, 122)), false, DungeonClass.TANK, 7, DungeonPhase.P3.S2),
        Terminal(listOf(BlockPos(59, 120, 122), BlockPos(59, 119, 123)), false, DungeonClass.MAGE, 8, DungeonPhase.P3.S2),
        Terminal(listOf(BlockPos(47, 109, 121), BlockPos(47, 109, 122)), false, DungeonClass.BERSERK, 9, DungeonPhase.P3.S2),
        Terminal(listOf(BlockPos(39, 108, 143), BlockPos(39, 108, 142)), false, DungeonClass.ARCHER, 10, DungeonPhase.P3.S2),
        Terminal(listOf(BlockPos(40, 124, 122), BlockPos(40, 124, 123)), false, DungeonClass.BERSERK, 11, DungeonPhase.P3.S2),
        Terminal(listOf(BlockPos(27, 124, 127), BlockPos(27, 125, 127)), true, DungeonClass.ARCHER, 12, DungeonPhase.P3.S2),
        Terminal(listOf(BlockPos(23, 132, 138), BlockPos(23, 133, 138)), true, DungeonClass.HEALER, 13, DungeonPhase.P3.S2),

        Terminal(listOf(BlockPos(-3, 109, 112), BlockPos(-2, 109, 112)), false, DungeonClass.TANK, 14, DungeonPhase.P3.S3),
        Terminal(listOf(BlockPos(-3, 119, 93), BlockPos(-2, 119, 93)), false, DungeonClass.HEALER, 15, DungeonPhase.P3.S3),
        Terminal(listOf(BlockPos(19, 123, 93), BlockPos(18, 123, 93)), false, DungeonClass.BERSERK, 16, DungeonPhase.P3.S3),
        Terminal(listOf(BlockPos(-3, 109, 77), BlockPos(-2, 109, 77)), false, DungeonClass.ARCHER, 17, DungeonPhase.P3.S3),
        Terminal(listOf(BlockPos(14, 122, 55), BlockPos(14, 123, 55)), true, DungeonClass.ARCHER, 18, DungeonPhase.P3.S3),
        Terminal(listOf(BlockPos(2, 122, 55), BlockPos(2, 123, 55)), true, DungeonClass.ARCHER, 19, DungeonPhase.P3.S3),

        Terminal(listOf(BlockPos(41, 109, 29), BlockPos(41, 109, 30)), false, DungeonClass.TANK, 20, DungeonPhase.P3.S4),
        Terminal(listOf(BlockPos(44, 121, 29), BlockPos(44, 121, 30)), false, DungeonClass.ARCHER, 21, DungeonPhase.P3.S4),
        Terminal(listOf(BlockPos(67, 109, 29), BlockPos(67, 109, 30)), false, DungeonClass.BERSERK, 22, DungeonPhase.P3.S4),
        Terminal(listOf(BlockPos(72, 115, 48), BlockPos(72, 114, 47)), false, DungeonClass.HEALER, 23, DungeonPhase.P3.S4),
        Terminal(listOf(BlockPos(86, 128, 46), BlockPos(86, 129, 46)), true, DungeonClass.HEALER, 24, DungeonPhase.P3.S4),
        Terminal(listOf(BlockPos(84, 121, 34), BlockPos(84, 122, 34)), true, DungeonClass.HEALER, 25, DungeonPhase.P3.S4)
    )

    override fun addConfig() {
        val feature = ConfigManager
            .addFeature(
                "Terminal waypoints",
                "Shows terminal and lever locations in F7/M7",
                "Floor 7",
                ConfigElement(
                    "terminalWaypoints",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Check dungeon class",
                ConfigElement(
                    "terminalWaypoints.focusMode",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Render text",
                ConfigElement(
                    "terminalWaypoints.renderText",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Highlight style",
                ConfigElement(
                    "terminalWaypoints.highlightStyle",
                    ElementType.Dropdown(
                        listOf("Outline", "Filled", "Both"),
                        0
                    )
                )
            )
            .addFeatureOption(
                "Terminal color",
                ConfigElement(
                    "terminalWaypoints.terminalColor",
                    ElementType.ColorPicker(Color(0, 255, 255, 200))
                )
            )
            .addFeatureOption(
                "Lever color",
                ConfigElement(
                    "terminalWaypoints.leverColor",
                    ElementType.ColorPicker(Color(255, 255, 0, 200))
                )
            )

        terminals.forEachIndexed { index, terminal ->
            val defaultIndex = classOptions.indexOf(terminal.defaultClass.displayName)

            val indexInSection = when (terminal.section.number) {
                1 -> index
                2 -> index - 6
                3 -> index - 13
                else -> index - 19
            }

            val sectionTerminals = when (terminal.section.number) {
                1 -> terminals.subList(0, 6)
                2 -> terminals.subList(6, 13)
                3 -> terminals.subList(13, 19)
                else -> terminals.subList(19, 25)
            }

            val name = if (terminal.isLever) {
                val leversInSection = sectionTerminals.filter { it.isLever }
                val leverIndex = leversInSection.indexOf(terminal)
                if (leverIndex == 0) "Right Lever" else "Left Lever"
            } else {
                val terminalIndex = sectionTerminals.take(indexInSection + 1).count { !it.isLever }
                "Terminal $terminalIndex"
            }

            feature.addFeatureOption(
                "S${terminal.section.number} $name class",
                ConfigElement(
                    "terminalWaypoints.terminal${index + 1}Class",
                    ElementType.Dropdown(classOptions, defaultIndex)
                )
            )
        }
    }

    override fun initialize() {
        register<RenderEvent.World.Last> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register

            val playerClass = DungeonAPI.dungeonClass
            val consumers = event.context.consumers()
            val matrices = event.context.matrixStack()

            terminals.filter { it.section == DungeonAPI.P3Phase }.forEach { terminal ->
                val allowedClass = getTerminalClass(terminal.configIndex, terminal.defaultClass)
                if (checkClass && playerClass != allowedClass) return@forEach

                val color = if (terminal.isLever) leverColor else terminalColor

                val boxPos = terminal.positions.first()
                when (highlightStyle) {
                    0 -> Render3D.drawOutlinedBB(
                        boxPos.center.toAabb(),
                        color,
                        consumers,
                        matrices,
                        true
                    )

                    1 -> Render3D.drawFilledBB(
                        boxPos.center.toAabb(),
                        color,
                        consumers,
                        matrices,
                        true
                    )

                    2 -> Render3D.drawSpecialBB(
                        boxPos.center.toAabb(),
                        color,
                        consumers,
                        matrices,
                        true
                    )
                }

                if (showText) {
                    val textPos = terminal.positions.last()
                    Render3D.drawString(
                        terminal.defaultClass.getFancyName(),
                        textPos.center,
                        matrices,
                        depth = false
                    )
                }
            }
        }
    }

    private fun DungeonClass.getFancyName(): String {
        return when (this) {
            DungeonClass.MAGE -> "§7[§bMage§7]"
            DungeonClass.ARCHER -> "§7[§6Archer§7]"
            DungeonClass.TANK -> "§7[§aTank§7]"
            DungeonClass.HEALER -> "§7[§dHealer§7]"
            DungeonClass.BERSERK -> "§7[§4Berserk§7]"
            else -> "§7[§8 ??? §7]"
        }
    }

    private fun Vec3.toAabb(): AABB {
        return AABB(
            x - 0.5,
            y - 0.5,
            z - 0.5,
            x + 0.5,
            y + 0.5,
            z + 0.5
        )
    }

    private fun getTerminalClass(configIndex: Int, defaultClass: DungeonClass): DungeonClass {
        val configValue = when (configIndex) {
            1 -> terminal1Class
            2 -> terminal2Class
            3 -> terminal3Class
            4 -> terminal4Class
            5 -> terminal5Class
            6 -> terminal6Class
            7 -> terminal7Class
            8 -> terminal8Class
            9 -> terminal9Class
            10 -> terminal10Class
            11 -> terminal11Class
            12 -> terminal12Class
            13 -> terminal13Class
            14 -> terminal14Class
            15 -> terminal15Class
            16 -> terminal16Class
            17 -> terminal17Class
            18 -> terminal18Class
            19 -> terminal19Class
            20 -> terminal20Class
            21 -> terminal21Class
            22 -> terminal22Class
            23 -> terminal23Class
            24 -> terminal24Class
            25 -> terminal25Class
            else -> return defaultClass
        }
        return classMapping[configValue] ?: defaultClass
    }
}