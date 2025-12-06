package xyz.meowing.krypt.features.solvers

import com.google.gson.JsonObject
import net.minecraft.world.level.block.Blocks
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils.getRealCoord
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.solvers.data.PuzzleTimer
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.modMessage
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color

/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2023-2025, odtheking
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Portions of this file are derived from OdinFabric
 * Copyright (c) odtheking
 * Licensed under BSD-3-Clause
 *
 * Modifications and additions:
 * Licensed under GPL-3.0
 */
@Module
object WaterBoardSolver : Feature(
    "waterBoardSolver",
    "Water board solver",
    "Shows optimal lever order and timing",
    "Solvers",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private lateinit var waterSolutions: JsonObject

    private val solutions = HashMap<LeverBlock, Array<Double>>()
    private var patternIdentifier = -1
    private var openedWaterTicks = -1
    private var tickCounter = 0

    private var roomCenter: BlockPos? = null
    private var rotation: Int? = null

    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null

    private val firstColor by config.colorPicker("First tracer color", Color(0, 255, 0, 255))
    private val secondTracerColor by config.colorPicker("Second tracer color", Color(255, 255, 0, 255))

    init {
        NetworkUtils.fetchJson<JsonObject>(
            url = "https://raw.githubusercontent.com/StellariumMC/zen-data/refs/heads/main/solvers/WaterSolver.json",
            onSuccess = {
                waterSolutions = it
                Krypt.LOGGER.info("Loaded Water Board solutions.")
            },
            onError = { error ->
                Krypt.LOGGER.error("Caught error while trying to load Water Board solutions: $error")
            }
        )
    }

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Water Board") return@register
            if (patternIdentifier != -1) return@register

            roomCenter = ScanUtils.getRoomCenter(event.new)
            rotation = 360 - (event.new.rotation.degrees)
            trueTimeStarted = System.currentTimeMillis()

            TickScheduler.Server.schedule(15) {
                KnitChat.modMessage("§7Solving Water Board...")
                // gaslight the user until we figure out a way to do it without the 1.5-2s delay
            }

            TickScheduler.Server.schedule(30) {
                solve()
            }
        }

        register<DungeonEvent.Room.Change> { event ->
            if (patternIdentifier != -1 && event.new.name != "Water Board") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<RenderEvent.World.Last> { event ->
            if (patternIdentifier == -1 || solutions.isEmpty()) return@register

            val solutionList = solutions
                .flatMap { (lever, times) -> times.drop(lever.clickCount).map { Pair(lever, it) } }
                .sortedBy { (lever, time) -> time + if (lever == LeverBlock.WATER) 0.01 else 0.0 }

            val firstSolution = solutionList.firstOrNull()?.first ?: return@register
            val firstPos = firstSolution.getLeverPos()

            Render3D.drawLineToPos(
                firstPos.toCenterVec(),
                event.context.consumers(),
                event.context.matrixStack(),
                floatArrayOf(
                    firstColor.red / 255f,
                    firstColor.green / 255f,
                    firstColor.blue / 255f
                ),
                firstColor.alpha / 255f
            )

            if (solutionList.size > 1) {
                val secondSolution = solutionList[1].first
                val secondPos = secondSolution.getLeverPos()
                if (firstPos != secondPos) {
                    Render3D.drawLine(
                        firstPos.toCenterVec(),
                        secondPos.toCenterVec(),
                        1f,
                        secondTracerColor,
                        event.context.consumers(),
                        event.context.matrixStack()
                    )
                }
            }

            solutions.forEach { (lever, times) ->
                val leverPos = lever.getLeverPos()
                times.drop(lever.clickCount).forEachIndexed { index, time ->
                    val timeInTicks = (time * 20).toInt()
                    val text = when (openedWaterTicks) {
                        -1 if timeInTicks == 0 -> "§a§lCLICK"
                        -1 -> {
                            when {
                                time < 2 -> "§c${String.format("%.1f", time)}s"
                                time < 6 -> "§e${String.format("%.1f", time)}s"
                                else -> "§a${String.format("%.1f", time)}s"
                            }
                        }
                        else -> {
                            val remainingTicks = openedWaterTicks + timeInTicks - tickCounter
                            if (remainingTicks > 0) {
                                val remainingSeconds = remainingTicks / 20.0
                                when {
                                    remainingSeconds < 2 -> "§c${String.format("%.1f", remainingSeconds)}s"
                                    remainingSeconds < 6 -> "§e${String.format("%.1f", remainingSeconds)}s"
                                    else -> "§a${String.format("%.1f", remainingSeconds)}s"
                                }
                            } else "§a§lCLICK"
                        }
                    }

                    Render3D.drawString(
                        text,
                        leverPos.toCenterVec().add(0.0, (index + lever.clickCount) * 0.5 + 1.0, 0.0),
                        event.context.matrixStack(),
                        scale = 1.35f,
                        depth = true
                    )
                }
            }
        }

        register<PacketEvent.Sent> { event ->
            if (solutions.isEmpty()) return@register
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register
            val position = packet.hitResult.blockPos

            LeverBlock.entries.find { it.getLeverPos() == position }?.let {
                if (it == LeverBlock.WATER && openedWaterTicks == -1) {
                    openedWaterTicks = tickCounter
                    if (timeStarted == null) timeStarted = System.currentTimeMillis()
                }

                it.clickCount++
            }

            val block = client.level?.getBlockState(position)?.block
            if (block == Blocks.CHEST && WoolColor.entries.all { !it.isClose() }) {
                val trueTime = trueTimeStarted ?: return@register
                val startTime = timeStarted ?: return@register

                val solveTime = (System.currentTimeMillis() - startTime).toDouble()
                val totalTime = (System.currentTimeMillis() - trueTime).toDouble()

                PuzzleTimer.submitTime("Water Board", solveTime, totalTime)
                reset()
            }
        }

        register<TickEvent.Server> {
            if (patternIdentifier == -1) return@register
            tickCounter++
        }
    }

    private fun solve() {
        val roomCenter = roomCenter ?: return
        val rotation = rotation ?: return

        val closeWalls = WoolColor.entries.joinToString("") {
            if (it.isClose()) it.ordinal.toString() else ""
        }.takeIf { it.length == 3 } ?: return

        patternIdentifier = when {
            getBlockAt(BlockPos(-1, 77, 12), roomCenter, rotation) == Blocks.TERRACOTTA -> 0
            getBlockAt(BlockPos(1, 78, 12), roomCenter, rotation) == Blocks.EMERALD_BLOCK -> 1
            getBlockAt(BlockPos(-1, 78, 12), roomCenter, rotation) == Blocks.DIAMOND_BLOCK -> 2
            getBlockAt(BlockPos(-1, 78, 12), roomCenter, rotation) == Blocks.QUARTZ_BLOCK -> 3
            else -> return KnitChat.modMessage("§cFailed to get Water Board pattern. Was the puzzle already started?")
        }

        solutions.clear()
        if (!::waterSolutions.isInitialized) {
            Krypt.LOGGER.error("[krypt] Water Board solutions not loaded")
            return
        }

        solutions.clear()
        waterSolutions.getAsJsonObject(patternIdentifier.toString())
            ?.getAsJsonObject(closeWalls)
            ?.entrySet()
            ?.forEach { entry ->
                val lever = when (entry.key) {
                    "diamond_block" -> LeverBlock.DIAMOND
                    "emerald_block" -> LeverBlock.EMERALD
                    "hardened_clay" -> LeverBlock.CLAY
                    "quartz_block" -> LeverBlock.QUARTZ
                    "gold_block" -> LeverBlock.GOLD
                    "coal_block" -> LeverBlock.COAL
                    "water" -> LeverBlock.WATER
                    else -> return@forEach
                }
                solutions[lever] = entry.value.asJsonArray.map { it.asDouble }.toTypedArray()
            }
    }

    private fun getBlockAt(pos: BlockPos, center: BlockPos, rot: Int): Block? {
        return client.level?.getBlockState(getRealCoord(pos, center, rot))?.block
    }

    private fun reset() {
        LeverBlock.entries.forEach { it.clickCount = 0 }
        patternIdentifier = -1
        solutions.clear()
        openedWaterTicks = -1
        tickCounter = 0
        roomCenter = null
        rotation = null
        trueTimeStarted = null
        timeStarted = null
    }

    private fun BlockPos.toCenterVec() = Vec3(x + 0.5, y + 0.5, z + 0.5)

    private enum class WoolColor(val relativePosition: BlockPos) {
        PURPLE(BlockPos(0, 56, 4)),
        ORANGE(BlockPos(0, 56, 3)),
        BLUE(BlockPos(0, 56, 2)),
        GREEN(BlockPos(0, 56, 1)),
        RED(BlockPos(0, 56, 0));

        fun isClose() = getBlockAt(relativePosition, roomCenter!!, rotation!!) != Blocks.AIR
    }

    private enum class LeverBlock(val relativePosition: BlockPos, var clickCount: Int = 0) {
        QUARTZ(BlockPos(5, 61, 5)),
        GOLD(BlockPos(5, 61, 0)),
        COAL(BlockPos(5, 61, -5)),
        DIAMOND(BlockPos(-5, 61, 5)),
        EMERALD(BlockPos(-5, 61, 0)),
        CLAY(BlockPos(-5, 61, -5)),
        WATER(BlockPos(0, 60, -10));

        fun getLeverPos() = getRealCoord(relativePosition, roomCenter!!, rotation!!)
    }
}