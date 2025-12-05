package xyz.meowing.krypt.features.general

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.events.core.PacketEvent
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.events.core.TickEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.LocationEvent
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

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
object MageBeam : Feature(
    "mageBeam",
    "Mage beam",
    "Customizes the rendering of the mage beam ability",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private data class BeamData(
        val id: Long = System.nanoTime(),
        val points: CopyOnWriteArrayList<Vec3> = CopyOnWriteArrayList(),
        var lastUpdateTick: Int = 0
    )

    private val activeBeams = CopyOnWriteArrayList<BeamData>()
    private var currentTick = 0

    private val customBeamEnabled by config.switch("Custom beam", true)
    private val duration by config.slider("Duration (ticks)", 40.0, 10.0, 100.0, false)
    private val color by config.colorPicker("Color", Color(255, 100, 100, 255))
    private val hideParticles by config.switch("Hide particles", true)
    private val minPoints by config.slider("Minimum points", 8.0, 1.0, 20.0, false)

    override fun initialize() {
        register<LocationEvent.WorldChange> {
            activeBeams.clear()
            currentTick = 0
        }

        register<TickEvent.Server> {
            currentTick++
        }

        register<PacketEvent.Received> { event ->
            val packet = event.packet as? ClientboundLevelParticlesPacket ?: return@register
            if (packet.particle != ParticleTypes.FIREWORK) return@register

            val newPoint = Vec3(packet.x, packet.y, packet.z)
            val recentBeam = activeBeams.lastOrNull()

            if (
                recentBeam != null &&
                (currentTick - recentBeam.lastUpdateTick) < 1 && 
                isPointInBeamDirection(recentBeam.points, newPoint)
            ) {
                recentBeam.points.add(newPoint)
                recentBeam.lastUpdateTick = currentTick
            } else {
                val newBeam = BeamData(points = CopyOnWriteArrayList<Vec3>().apply { add(newPoint) }, lastUpdateTick = currentTick)
                activeBeams.add(newBeam)

                TickScheduler.Server.schedule(duration.toLong()) {
                    activeBeams.removeAll { it.id == newBeam.id }
                }
            }

            if (hideParticles) event.cancel()
        }

        register<RenderEvent.World.Last> { event ->
            if (!customBeamEnabled) return@register

            activeBeams.forEach { beam ->
                if (beam.points.size < minPoints.toInt() || beam.points.isEmpty()) return@forEach

                Render3D.drawLine(
                    beam.points.first(),
                    beam.points.last(),
                    1f,
                    color,
                    event.context.consumers(),
                    event.context.matrixStack()
                )
            }
        }
    }

    private fun isPointInBeamDirection(points: List<Vec3>, newPoint: Vec3): Boolean {
        if (points.isEmpty() || points.size <= 1) return true
        val first = points.first()
        val last = points.last()
        val direction = last.subtract(first).normalize()
        val toNew = newPoint.subtract(last).normalize()
        return direction.dot(toNew) > 0.99
    }
}