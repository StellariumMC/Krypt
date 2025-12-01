package xyz.meowing.krypt.features.general

import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ConfigDelegate
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.hud.HUDEditor
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.Utils.toTimerFormat
import xyz.meowing.krypt.utils.rendering.Render2D
import xyz.meowing.krypt.utils.rendering.Render2D.pushPop
import java.awt.Color

@Module
object SplitTimers : Feature(
    "splitTimers",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Split Timers"

    private var ticks = 0L

    private var timerStyle by ConfigDelegate<Int>("splitTimers.timerStyle")

    private val shownList = mutableListOf<Split>()

    private class Split(
        val splitName: String,
        val startTrigger: String,
        val floor: DungeonFloor? = null,
        val master: Boolean = false,
        val weird: Boolean = false
    ) {
        var startTime = 0L
        var endTime = 0L
        var startTick = 0L
        var endTick = 0L

        var hasStarted = false
        var hasFinished = false
        var inSplit = false

        init {
            shownList.add(this)
        }
    }

    override fun addConfig() {
        ConfigManager
            .addFeature("Split timers",
                "Shows the splits times of your dungeon run.",
                "General",
                ConfigElement(
                    "splitTimers",
                    ElementType.Switch(false)
                )
            )
            .addFeatureOption(
                "Timer style",
                ConfigElement(
                    "splitTimers.timerStyle",
                    ElementType.Dropdown(
                        listOf("Milliseconds", "Ticks", "Both"),
                        0
                    )
                )
            )
            .addFeatureOption("HudEditor",
                ConfigElement(
                    "splitTimers.hudEditor",
                    ElementType.Button("Edit Position") {
                        TickScheduler.Client.post {
                            client.execute { client.setScreen(HUDEditor()) }
                        }
                    }
                )
            )
    }

    override fun initialize() {
        initSplits()
        HUDManager.registerCustom(NAME, 160, 30, this::hudEditorRender, "splitTimers")

        register<GuiEvent.Render.HUD> {
            renderHud(it.context)
        }

        register<TickEvent.Server> {
            ticks++
        }

        register<ChatEvent.Receive> { event ->
            val message = event.message.string

            shownList.filter { !it.hasFinished }.forEach {
                if (
                    message.startsWith(it.startTrigger) ||
                    (it.weird && message.startsWith("[BOSS]") && message.endsWith("Livid: Impossible! How did you figure out which one I was?!"))
                    ) {
                    shownList.filter { split -> split.inSplit }.forEach { split ->
                        split.inSplit = false
                        split.endTime = System.currentTimeMillis()
                        split.endTick = ticks
                        split.hasFinished = true
                    }

                    it.hasStarted = true
                    it.startTime = System.currentTimeMillis()
                    it.startTick = ticks
                    it.inSplit = true
                }
            }
        }

        register<DungeonEvent.End> {
            shownList.filter { split -> split.inSplit }.forEach { split ->
                split.inSplit = false
                split.endTime = System.currentTimeMillis()
                split.endTick = ticks
                split.hasFinished = true
            }
        }

        register<LocationEvent.WorldChange> {
            shownList.forEach {
                it.hasFinished = false
                it.hasStarted = false
                it.startTick = 0L
                it.startTime = 0L
                it.endTime = 0L
                it.endTick = 0L
                it.inSplit = false
            }
        }
    }

    fun hudEditorRender(context: GuiGraphics) {
        context.pushPop {
            val text = when (timerStyle) {
                2 -> "§7[41.45]"
                else -> ""
            }

            Render2D.renderStringWithShadow(context, "Split 1", 0f, 0f, 1f, Color(255, 27, 141).rgb)
            Render2D.renderStringWithShadow(context, "Split 2", 0f, 10f, 1f, Color(247, 209, 0).rgb)
            Render2D.renderStringWithShadow(context, "Split 3", 0f, 20f, 1f, Color(32, 171, 247).rgb)

            Render2D.renderStringWithShadow(context, "1m 01.41s $text", 60f, 0f, 1f)
            Render2D.renderStringWithShadow(context, "41.41s $text", 60f, 10f, 1f)
            Render2D.renderStringWithShadow(context, "21.21s $text", 60f, 20f, 1f)
        }
    }

    private fun renderHud(context: GuiGraphics) {
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        val renderedList = shownList.filter { it.hasStarted }
        renderedList.forEachIndexed { index, split ->
            val name = split.splitName
            val timerString = run {
                val msTimeStr: String
                val tickTimeStr: String

                if (split.inSplit) {
                    msTimeStr = ((System.currentTimeMillis() - split.startTime) / 1000f).toTimerFormat()
                    tickTimeStr = ((ticks - split.startTick) / 20f).toTimerFormat()
                } else {
                    msTimeStr = ((split.endTime - split.startTime) / 1000f).toTimerFormat()
                    tickTimeStr = ((split.endTick - split.startTick) / 20f).toTimerFormat()
                }

                when (timerStyle) {
                    0 -> msTimeStr
                    1 -> tickTimeStr
                    else -> "$msTimeStr §7[$tickTimeStr]"
                }
            }

            Render2D.renderStringWithShadow(context, name, x, y + index * 10 * scale, scale)
            Render2D.renderStringWithShadow(context, timerString, x + 100 * scale, y + index * 10 * scale, scale)
        }
    }

    private fun initSplits() {
        Split("§2Blood Opened", "§e[NPC] §bMort§f: Here, I found this map when I first entered the dungeon.")
        Split("§bBlood Cleared", "[BOSS] The Watcher: ")
        Split("§dPortal", "[BOSS] The Watcher: You have proven yourself. You may pass.", DungeonFloor.F7)

        // F1
        Split("§cFirst Kill", "[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable.", DungeonFloor.F1)
        Split("§cSecond Kill", "[BOSS] Bonzo: Oh noes, you got me.. what ever will I do?!", DungeonFloor.F1)
        Split("§eEnd Dialogue", "[BOSS] Bonzo: Alright, maybe I'm just weak after all..", DungeonFloor.F1)

        // F2
        Split("§cUndeads", "[BOSS] Scarf: This is where the journey ends for you, Adventurers.", DungeonFloor.F2)
        Split("§bScarf", "[BOSS] Scarf: Those toys are not strong enough I see.", DungeonFloor.F2)

        // F3
        Split("§9Guardians" , "[BOSS] The Professor: I was burdened with terrible news recently...", DungeonFloor.F3)
        Split("§eThe Professor" , "[BOSS] The Professor: Oh? You found my Guardians' one weakness?", DungeonFloor.F3)
        Split("§bGuardifessor" , "[BOSS] The Professor: I see. You have forced me to use my ultimate technique.", DungeonFloor.F3)

        // F4
        Split("§eDialogue" , "[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!", DungeonFloor.F4)
        Split("§bThorn Kill" , "[BOSS] Thorn: Dance! Dance with my Spirit animals! And may you perish in a delightful way!", DungeonFloor.F4)

        // F5
        Split("§eStart Dialogue" , "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows.", DungeonFloor.F5)
        Split("§cLivid Kill" , "[BOSS] Livid: I respect you for making it to here, but I'll be your undoing.", DungeonFloor.F5)
        Split("§eEnd Dialogue" , "This one uses regex so heres is some placeholder text, haiiiii", DungeonFloor.F5, weird = true)

        // F6
        Split("§6Terracottas" , "[BOSS] Sadan: So you made it all the way here... Now you wish to defy me? Sadan?!", DungeonFloor.F6)
        Split("§dGiants" , "[BOSS] Sadan: ENOUGH!", DungeonFloor.F6)
        Split("§cSadan" , "[BOSS] Sadan: You did it. I understand now, you have earned my respect.", DungeonFloor.F6)


        // F7
        Split("§5Maxor", "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!", DungeonFloor.F7)
        Split("§3Storm", "[BOSS] Storm: Pathetic Maxor, just like expected.", DungeonFloor.F7)
        Split("§6Terminals", "[BOSS] Goldor: Who dares trespass into my domain?", DungeonFloor.F7)
        Split("§7Golor", "The Core entrance is opening!", DungeonFloor.F7)
        Split("§cNecron", "[BOSS] Necron: You went further than any human before, congratulations.", DungeonFloor.F7)
        Split("§4Dragons", "[BOSS] Necron: All this, for nothing...", DungeonFloor.F7, true)
    }
}
