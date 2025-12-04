package xyz.meowing.krypt.features.floor7

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonClass
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonPhase
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

@Module
object TerminalWaypoints : Feature(
    configKey = "terminalWaypoints",
    configName = "Terminal waypoints",
    configDescription = "Shows terminal and lever locations in F7/M7",
    configCategory = "Floor 7",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val classOptions = listOf(
        "Healer",
        "Mage",
        "Berserk",
        "Archer",
        "Tank"
    )

    private data class Terminal(
        val positions: List<BlockPos>,
        val isLever: Boolean,
        val defaultClass: DungeonClass,
        val configIndex: Int,
        val section: DungeonPhase.P3
    )

    private val classMapping = mapOf(
        0 to DungeonClass.HEALER,
        1 to DungeonClass.MAGE,
        2 to DungeonClass.BERSERK,
        3 to DungeonClass.ARCHER,
        4 to DungeonClass.TANK
    )

    private val checkClass by config.switch("Check dungeon class")
    private val showText by config.switch("Render text", true)
    private val highlightStyle by config.dropdown("Highlight style", listOf("Outline", "Filled", "Both"), 0)
    private val terminalColor by config.colorPicker("Terminal color", Color(0, 255, 255, 200))
    private val leverColor by config.colorPicker("Lever color", Color(255, 255, 0, 200))

    private val terminal1Class by config.dropdown("S1 Terminal 1 class", classOptions, 4)
    private val terminal2Class by config.dropdown("S1 Terminal 2 class", classOptions, 4)
    private val terminal3Class by config.dropdown("S1 Terminal 3 class", classOptions, 1)
    private val terminal4Class by config.dropdown("S1 Terminal 4 class", classOptions, 1)
    private val terminal5Class by config.dropdown("S1 Right Lever class", classOptions, 3)
    private val terminal6Class by config.dropdown("S1 Left Lever class", classOptions, 3)

    private val terminal7Class by config.dropdown("S2 Terminal 1 class", classOptions, 4)
    private val terminal8Class by config.dropdown("S2 Terminal 2 class", classOptions, 1)
    private val terminal9Class by config.dropdown("S2 Terminal 3 class", classOptions, 2)
    private val terminal10Class by config.dropdown("S2 Terminal 4 class", classOptions, 3)
    private val terminal11Class by config.dropdown("S2 Terminal 5 class", classOptions, 2)
    private val terminal12Class by config.dropdown("S2 Right Lever class", classOptions, 3)
    private val terminal13Class by config.dropdown("S2 Left Lever class", classOptions, 0)

    private val terminal14Class by config.dropdown("S3 Terminal 1 class", classOptions, 4)
    private val terminal15Class by config.dropdown("S3 Terminal 2 class", classOptions, 0)
    private val terminal16Class by config.dropdown("S3 Terminal 3 class", classOptions, 2)
    private val terminal17Class by config.dropdown("S3 Terminal 4 class", classOptions, 3)
    private val terminal18Class by config.dropdown("S3 Right Lever class", classOptions, 3)
    private val terminal19Class by config.dropdown("S3 Left Lever class", classOptions, 3)

    private val terminal20Class by config.dropdown("S4 Terminal 1 class", classOptions, 4)
    private val terminal21Class by config.dropdown("S4 Terminal 2 class", classOptions, 3)
    private val terminal22Class by config.dropdown("S4 Terminal 3 class", classOptions, 2)
    private val terminal23Class by config.dropdown("S4 Terminal 4 class", classOptions, 0)
    private val terminal24Class by config.dropdown("S4 Right Lever class", classOptions, 0)
    private val terminal25Class by config.dropdown("S4 Left Lever class", classOptions, 0)

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