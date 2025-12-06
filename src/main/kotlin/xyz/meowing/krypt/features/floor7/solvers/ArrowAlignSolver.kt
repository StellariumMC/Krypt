package xyz.meowing.krypt.features.floor7.solvers

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import tech.thatgravyboat.skyblockapi.utils.extentions.component1
import tech.thatgravyboat.skyblockapi.utils.extentions.component2
import tech.thatgravyboat.skyblockapi.utils.extentions.component3
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonPhase
import xyz.meowing.krypt.events.core.EntityEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D

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
object ArrowAlignSolver : Feature(
    "arrowAlignSolver",
    "Arrow-Align solver",
    "Shows clicks needed for Arrow Align in F7/M7",
    "Floor 7",
    dungeonFloor = listOf(DungeonFloor.F7, DungeonFloor.M7)
) {
    private val blockWrong by config.switch("Block wrong clicks")
    private val blockWrongType by config.dropdown("Block when", listOf("Always", "When crouching", "Not crouching"))

    private val checkPos = Vec3(0.0, 120.0, 77.0)
    private val frameGridCorner = BlockPos(-2, 120, 75)
    private val recentClickTimestamps = mutableMapOf<Int, Long>()
    private val clicksRemaining = mutableMapOf<Int, Int>()
    private var currentFrameRotations: List<Int>? = null
    private var targetSolution: IntArray? = null

    private val possibleSolutions = listOf(
        intArrayOf(7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, -1, -1, 7, 1),
        intArrayOf(-1, -1, 7, 7, 5, -1, 7, 1, -1, 5, -1, -1, -1, -1, -1, -1, 7, 5, -1, 1, -1, -1, 7, 7, 1),
        intArrayOf(7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, -1, 7, 5, -1, -1, -1, -1, 5, -1, -1, -1, 3, 3),
        intArrayOf(5, 3, 3, 3, -1, 5, -1, -1, -1, -1, 7, 7, -1, -1, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, -1),
        intArrayOf(5, 3, 3, 3, 3, 5, -1, -1, -1, 1, 7, 7, -1, -1, 1, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1),
        intArrayOf(7, 7, 7, 7, -1, 1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, -1, 7, 7, 7, 1),
        intArrayOf(-1, -1, -1, -1, -1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1),
        intArrayOf(-1, -1, -1, -1, -1, 1, 3, 3, 3, 3, -1, -1, -1, -1, 1, 7, 7, 7, 7, 1, -1, -1, -1, -1, -1),
        intArrayOf(-1, -1, -1, -1, -1, -1, 1, -1, 1, -1, 7, 1, 7, 1, 3, 1, -1, 1, -1, 1, -1, -1, -1, -1, -1)
    )

    override fun initialize() {
        register<TickEvent.Client> {
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register

            val player = client.player ?: return@register

            if (player.position().distanceTo(checkPos) > 14) {
                currentFrameRotations = null
                targetSolution = null
                clicksRemaining.clear()
                return@register
            }

            currentFrameRotations = getFrames()

            possibleSolutions.forEach { arr ->
                for (i in arr.indices) {
                    val currentRotation = currentFrameRotations?.get(i) ?: -1
                    if ((arr[i] == -1 || currentRotation == -1) && arr[i] != currentRotation) return@forEach
                }

                targetSolution = arr

                for (i in arr.indices) {
                    val currentRotation = currentFrameRotations?.get(i) ?: return@forEach
                    val clicksNeeded = calculateClicksNeeded(currentRotation, arr[i])
                    if (clicksNeeded == 0) continue
                    clicksRemaining[i] = clicksNeeded
                }
            }
        }

        register<EntityEvent.Interact> { event ->
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register

            val entity = event.entity as? ItemFrame ?: return@register
            if (entity.item?.item != Items.ARROW) return@register
            val (x, y, z) = entity.blockPosition()

            val frameIndex = ((y - frameGridCorner.y) + (z - frameGridCorner.z) * 5)
            if (x != frameGridCorner.x) return@register
            if (currentFrameRotations?.get(frameIndex) == -1) return@register
            if (frameIndex !in 0..24) return@register

            if (!clicksRemaining.containsKey(frameIndex) && blockWrong) {
                val shouldBlock = when (blockWrongType) {
                    0 -> true
                    1 -> client.player?.isShiftKeyDown == true
                    2 -> client.player?.isShiftKeyDown == false
                    else -> false
                }

                if (shouldBlock) {
                    event.cancel()
                    return@register
                }
            }

            recentClickTimestamps[frameIndex] = System.currentTimeMillis()
            currentFrameRotations = currentFrameRotations?.toMutableList()?.apply { this[frameIndex] = (this[frameIndex] + 1) % 8 }

            val currentRotation = currentFrameRotations?.get(frameIndex) ?: return@register
            val targetRotation = targetSolution?.get(frameIndex) ?: return@register

            if (calculateClicksNeeded(currentRotation, targetRotation) == 0) {
                clicksRemaining.remove(frameIndex)
            }
        }

        register<RenderEvent.World.Last> { event ->
            if (clicksRemaining.isEmpty()) return@register
            if (DungeonAPI.F7Phase != DungeonPhase.F7.P3) return@register

            clicksRemaining.forEach { (index, clickNeeded) ->
                val colorCode = when {
                    clickNeeded == 0 -> return@forEach
                    clickNeeded < 3 -> 'a'
                    clickNeeded < 5 -> '6'
                    else -> 'c'
                }

                Render3D.drawString(
                    "§$colorCode$clickNeeded",
                    getFramePositionFromIndex(index).center.add(-0.3, 0.1, 0.0),
                    event.context.matrixStack()
                )
            }
        }
    }

    private fun getFrames(): List<Int> {
        val itemFrames = client.level?.entitiesForRendering()
            ?.filterIsInstance<ItemFrame>()
            ?.filter { it.item?.item?.asItem() == Items.ARROW }
            ?.takeIf { it.isNotEmpty() }
            ?: return List(25) { -1 }

        return (0..24).map { index ->
            if (
                recentClickTimestamps[index]?.let { System.currentTimeMillis() - it < 1000 } == true &&
                currentFrameRotations != null
            ) {
                currentFrameRotations?.get(index) ?: -1
            } else {
                itemFrames.find { it.blockPosition() == getFramePositionFromIndex(index) }?.rotation ?: -1
            }
        }
    }

    private fun getFramePositionFromIndex(index: Int) = frameGridCorner.offset(0, index % 5, index / 5)
    private fun calculateClicksNeeded(currentRotation: Int, targetRotation: Int) = (8 - currentRotation + targetRotation) % 8
}