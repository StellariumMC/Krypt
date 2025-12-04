package xyz.meowing.krypt.features.solvers

import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import net.minecraft.world.level.block.Blocks
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.Block
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.events.core.WorldEvent
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.TitleUtils
import xyz.meowing.krypt.utils.Utils.toFloatArray
import xyz.meowing.krypt.utils.glowThisFrame
import xyz.meowing.krypt.utils.glowingColor
import xyz.meowing.krypt.utils.rendering.Render2D
import java.awt.Color

@Module
object LividSolver : Feature(
    "lividSolver",
    "Livid solver",
    "Shows the correct Livid in F5/M5",
    "Solvers",
    dungeonFloor = listOf(DungeonFloor.F5, DungeonFloor.M5)
) {
    private var currentLivid = Livid.HOCKEY
    private val lividPos = BlockPos(5, 108, 42)
    private var started = false
    private var ticks = 390

    private const val NAME = "Ice Spray Timer"
    private const val START_MESSAGE = "[BOSS] Livid: Welcome, you've arrived right on time. I am Livid, the Master of Shadows."

    private enum class Livid(
        val entityName: String,
        val block: Block
    ) {
        VENDETTA("Vendetta", Blocks.WHITE_STAINED_GLASS),
        CROSSED("Crossed", Blocks.MAGENTA_STAINED_GLASS),
        ARCADE("Arcade", Blocks.YELLOW_STAINED_GLASS),
        SMILE("Smile", Blocks.LIME_STAINED_GLASS),
        DOCTOR("Doctor", Blocks.GRAY_STAINED_GLASS),
        PURPLE("Purple", Blocks.PURPLE_STAINED_GLASS),
        SCREAM("Scream", Blocks.BLUE_STAINED_GLASS),
        FROG("Frog", Blocks.GREEN_STAINED_GLASS),
        HOCKEY("Hockey", Blocks.RED_STAINED_GLASS)
        ;

        var entity: Player? = null
    }

    private val color by config.colorPicker("Color", Color(0, 255, 255, 127))
    private val line by config.switch("Tracer", false)
    private val iceSprayTimer by config.switch("Ice spray timer", false)
    private val ticksInsteadOfTime by config.switch("Ticks instead of time", true)

    override fun initialize() {
        HUDManager.register(NAME, "§bIce spray in: §f13.4s", "lividSolver.iceSprayTimer")

        createCustomEvent<RenderEvent.Entity.Pre>("renderLivid") { event ->
            val entity = event.entity

            if (currentLivid.entity == entity && player?.hasLineOfSight(entity) == true) {
                entity.glowThisFrame = true
                entity.glowingColor = color.rgb
            }
        }

        createCustomEvent<RenderEvent.World.Last>("renderLine") { event ->
            currentLivid.entity?.let { entity ->
                Render3D.drawLineToEntity(
                    entity,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    color.toFloatArray(),
                    color.alpha.toFloat()
                )
            }
        }

        register<WorldEvent.BlockUpdate> { event ->
            if (event.pos != lividPos) return@register

            currentLivid = Livid.entries.find { it.block.defaultBlockState() == event.new.block.defaultBlockState() }
                ?: return@register
            registerRender()
        }

        register<EntityEvent.Packet.Metadata> { event ->
            if (!DungeonAPI.inBoss) return@register

            val blindnessDuration = client.player?.getEffect(MobEffects.BLINDNESS)?.duration ?: 0
            val delay = (blindnessDuration - 20).coerceAtLeast(1).toLong()

            TickScheduler.Client.schedule(delay) {
                val player = event.entity as? Player ?: return@schedule
                if (player.name.stripped == "${currentLivid.entityName} Livid") currentLivid.entity = player
            }
        }

        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            if (!iceSprayTimer) return@register
            if (event.message.stripped != START_MESSAGE) return@register

            started = true
        }

        register<LocationEvent.WorldChange> {
            unregisterRender()
        }

        register<TickEvent.Server> {
            if (!started) return@register

            ticks--

            if (ticks == 0) {
                started = false
                ticks = 390
                TitleUtils.showTitle("§bIce spray livid!", duration = 1000)
            }
        }

        register<GuiEvent.Render.HUD> { event ->
            if (!started) return@register

            val x = HUDManager.getX(NAME)
            val y = HUDManager.getY(NAME)
            val scale = HUDManager.getScale(NAME)
            val time = if (ticksInsteadOfTime) "$ticks" else "${ticks / 20}s"

            Render2D.renderString(event.context, "§bIce spray in: §f$time", x, y, scale)
        }
    }

    private fun registerRender() {
        registerEvent("renderLivid")
        if (line) registerEvent("renderLine")
    }

    private fun unregisterRender() {
        unregisterEvent("renderLivid")
        unregisterEvent("renderLine")

        currentLivid = Livid.HOCKEY
        currentLivid.entity = null
        started = false
        ticks = 390
    }
}