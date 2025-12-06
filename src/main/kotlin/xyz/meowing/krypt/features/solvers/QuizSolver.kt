package xyz.meowing.krypt.features.solvers

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils.getRealCoord
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.GuiEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.solvers.data.PuzzleTimer
import xyz.meowing.krypt.hud.HUDManager
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.Utils.toTimerFormat
import xyz.meowing.krypt.utils.rendering.Render2D
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
object QuizSolver : Feature(
    "quizSolver",
    "Quiz solver",
    "Highlights correct trivia answers",
    "Solvers",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private const val NAME = "Quiz Solver"

    private data class TriviaAnswer(var blockPos: BlockPos?, var isCorrect: Boolean)

    private val quizSolutions = mutableMapOf<String, List<String>>()
    private var triviaAnswers: List<String>? = null
    private val triviaOptions = MutableList(3) { TriviaAnswer(null, false) }

    private var inQuiz = false
    private var roomCenter: BlockPos? = null
    private var rotation: Int? = null

    private val boxColor by config.colorPicker("Box color", Color(0, 255, 0, 127))
    private val beam by config.switch("Show beam", true)
    private val timer by config.switch("Timer")

    private var answerTime: Int = 0
    private var questionsStarted = false
    private var timeStarted: Long? = null

    init {
        NetworkUtils.fetchJson<Map<String, List<String>>>(
            url = "https://raw.githubusercontent.com/StellariumMC/zen-data/refs/heads/main/solvers/QuizSolver.json",
            onSuccess = {
                quizSolutions.putAll(it)
                Krypt.LOGGER.info("Loaded Quiz solutions.")
            },
            onError = { error ->
                Krypt.LOGGER.error("Caught error while trying to load Quiz solutions: $error")
            }
        )
    }

    override fun initialize() {
        HUDManager.register(NAME, "§5Quiz §f: §c10.45s", "quizSolver.timer")

        register<GuiEvent.Render.HUD> { renderHud(it.context) }

        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Quiz") return@register

            inQuiz = true
            roomCenter = ScanUtils.getRoomCenter(event.new)
            rotation = 360 - event.new.rotation.degrees
            timeStarted = System.currentTimeMillis()

            triviaOptions[0].blockPos = getRealCoord(BlockPos(5, 70, -9), roomCenter!!, rotation!!)
            triviaOptions[1].blockPos = getRealCoord(BlockPos(0, 70, -6), roomCenter!!, rotation!!)
            triviaOptions[2].blockPos = getRealCoord(BlockPos(-5, 70, -9), roomCenter!!, rotation!!)
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inQuiz && event.new.name != "Quiz") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<ChatEvent.Receive> { event ->
            if (event.isActionBar) return@register
            val message = event.message.stripped
            val trimmed = message.trim()

            when {
                message.startsWith("[STATUE] Oruo the Omniscient: ") && message.endsWith("correctly!") -> {
                    answerTime = (7.5 * 20).toInt()
                    if (message.contains("answered the final question")) {
                        questionsStarted = false

                        val startTime = timeStarted ?: return@register
                        val completionTime = (System.currentTimeMillis() - startTime).toDouble()

                        PuzzleTimer.submitTime("Quiz", completionTime, completionTime)
                        reset()
                    } else if (message.contains("answered Question #")) {
                        triviaOptions.forEach { it.isCorrect = false }
                    }
                }

                trimmed.startsWith("ⓐ") || trimmed.startsWith("ⓑ") || trimmed.startsWith("ⓒ") -> {
                    triviaAnswers?.firstOrNull { message.endsWith(it) }?.let {
                        when (trimmed[0]) {
                            'ⓐ' -> triviaOptions[0].isCorrect = true
                            'ⓑ' -> triviaOptions[1].isCorrect = true
                            'ⓒ' -> triviaOptions[2].isCorrect = true
                        }
                    }
                }

                message == "[STATUE] Oruo the Omniscient: I am Oruo the Omniscient. I have lived many lives. I have learned all there is to know." -> {
                    if (inQuiz) questionsStarted = true
                    answerTime = 11 * 20
                }

                else -> {
                    val newAnswers = when {
                        trimmed == "What SkyBlock year is it?" -> {
                            val year = (((System.currentTimeMillis() / 1000) - 1560276000) / 446400).toInt() + 1
                            listOf("Year $year")
                        }
                        else -> quizSolutions.entries.find { message.contains(it.key) }?.value
                    }
                    newAnswers?.let { triviaAnswers = it }
                }
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (!inQuiz || triviaAnswers == null) return@register

            triviaOptions.forEach { answer ->
                if (!answer.isCorrect) return@forEach
                val pos = answer.blockPos ?: return@forEach

                Render3D.drawSpecialBB(
                    AABB(pos),
                    boxColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    false
                )

                if (beam) {
                    Render3D.renderBeam(
                        event.context,
                        pos.x.toDouble(),
                        pos.y.toDouble(),
                        pos.z.toDouble(),
                        boxColor,
                        false
                    )
                }
            }
        }

        register<TickEvent.Server> {
            if (answerTime > 0) answerTime--
        }
    }

    private fun reset() {
        inQuiz = false
        roomCenter = null
        rotation = null
        triviaOptions.forEach {
            it.blockPos = null
            it.isCorrect = false
        }
        triviaAnswers = null
        questionsStarted = false
        answerTime = 0
        timeStarted = null
    }

    private fun renderHud(context: GuiGraphics) {
        val x = HUDManager.getX(NAME)
        val y = HUDManager.getY(NAME)
        val scale = HUDManager.getScale(NAME)

        if (timer && questionsStarted) {
            val timer = when {
                answerTime > 0 -> "§c${(answerTime / 20f).toTimerFormat()}"
                else -> "§aReady"
            }
            val text = "§5Quiz §f: $timer"
            Render2D.renderStringWithShadow(context, text, x, y, scale)
        }
    }
}