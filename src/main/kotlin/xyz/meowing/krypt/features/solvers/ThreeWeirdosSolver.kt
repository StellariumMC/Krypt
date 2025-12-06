package xyz.meowing.krypt.features.solvers

import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.core.BlockPos
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.utils.ScanUtils.rotateBlock
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.events.core.DungeonEvent
import xyz.meowing.krypt.events.core.LocationEvent
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.features.solvers.data.PuzzleTimer
import xyz.meowing.krypt.utils.rendering.Render3D
import java.awt.Color
import java.util.concurrent.ConcurrentHashMap

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
object ThreeWeirdosSolver : Feature(
    "weirdosSolver",
    "Three weirdos solver",
    "Highlights the correct chest and removes wrong ones",
    "Solvers",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private val npcRegex = Regex("\\[NPC] (\\w+): (.+)")

    private var inWeirdos = false
    private var rotation = 0
    private var correctPos: BlockPos? = null
    private val wrongPositions = ConcurrentHashMap.newKeySet<BlockPos>()

    private var trueTimeStarted: Long? = null
    private var timeStarted: Long? = null

    private val correctColor by config.colorPicker("Correct chest color", Color(0, 255, 0, 127))
    private val wrongColor by config.colorPicker("Wrong chest color", Color(255, 0, 0, 127))
    private val highlightWrongChests by config.switch("Highlight wrong chests", true)

    override fun initialize() {
        register<DungeonEvent.Room.Change> { event ->
            if (event.new.name != "Three Weirdos") return@register

            inWeirdos = true
            rotation = 360 - (event.new.rotation.degrees)
            trueTimeStarted = System.currentTimeMillis()
        }

        register<DungeonEvent.Room.Change> { event ->
            if (inWeirdos && event.new.name != "Three Weirdos") reset()
        }

        register<LocationEvent.WorldChange> { reset() }

        register<RenderEvent.World.Last> { event ->
            if (!inWeirdos) return@register

            correctPos?.let { chest ->
                Render3D.drawSpecialBB(
                    chest,
                    correctColor,
                    event.context.consumers(),
                    event.context.matrixStack(),
                    phase = false
                )
            }

            if (highlightWrongChests) {
                wrongPositions.forEach { pos ->
                    Render3D.drawSpecialBB(
                        pos,
                        wrongColor,
                        event.context.consumers(),
                        event.context.matrixStack(),
                        phase = false
                    )
                }
            }
        }

        register<ChatEvent.Receive> { event ->
            if (!inWeirdos || event.isActionBar) return@register

            val match = npcRegex.find(event.message.stripped) ?: return@register
            val (npc, msg) = match.destructured

            if (solutions.none { it.matches(msg) } && wrong.none { it.matches(msg) }) return@register

            if (timeStarted == null) timeStarted = System.currentTimeMillis()

            val world = KnitClient.world ?: return@register
            val correctNPC = world.entitiesForRendering()
                .filterIsInstance<ArmorStand>()
                .find { it.name.stripped == npc } ?: return@register

            val pos = BlockPos(
                (correctNPC.x - 0.5).toInt(),
                69,
                (correctNPC.z - 0.5).toInt()
            )
                .offset(BlockPos(1, 0, 0)
                .rotateBlock(rotation))

            if (solutions.any { it.matches(msg) }) {
                correctPos = pos
            } else {
                wrongPositions.add(pos)
            }
        }

        register<PacketEvent.Sent> { event ->
            if (!inWeirdos || wrongPositions.size != 2) return@register
            val packet = event.packet as? ServerboundUseItemOnPacket ?: return@register
            if (packet.hitResult.blockPos != correctPos) return@register

            val trueTime = trueTimeStarted ?: return@register
            val startTime = timeStarted ?: return@register

            val solveTime = (System.currentTimeMillis() - startTime).toDouble()
            val totalTime = (System.currentTimeMillis() - trueTime).toDouble()

            PuzzleTimer.submitTime("Three Weirdos", solveTime, totalTime)
            reset()
        }
    }

    private fun reset() {
        inWeirdos = false
        rotation = 0
        correctPos = null
        wrongPositions.clear()
        trueTimeStarted = null
        timeStarted = null
    }

    private val solutions = listOf(
        Regex("The reward is not in my chest!"),
        Regex("At least one of them is lying, and the reward is not in .+'s chest.?"),
        Regex("My chest doesn't have the reward. We are all telling the truth.?"),
        Regex("My chest has the reward and I'm telling the truth!"),
        Regex("The reward isn't in any of our chests.?"),
        Regex("Both of them are telling the truth. Also, .+ has the reward in their chest.?"),
    )

    private val wrong = listOf(
        Regex("One of us is telling the truth!"),
        Regex("They are both telling the truth. The reward isn't in .+'s chest."),
        Regex("We are all telling the truth!"),
        Regex(".+ is telling the truth and the reward is in his chest."),
        Regex("My chest doesn't have the reward. At least one of the others is telling the truth!"),
        Regex("One of the others is lying."),
        Regex("They are both telling the truth, the reward is in .+'s chest."),
        Regex("They are both lying, the reward is in my chest!"),
        Regex("The reward is in my chest."),
        Regex("The reward is not in my chest. They are both lying."),
        Regex(".+ is telling the truth."),
        Regex("My chest has the reward.")
    )
}