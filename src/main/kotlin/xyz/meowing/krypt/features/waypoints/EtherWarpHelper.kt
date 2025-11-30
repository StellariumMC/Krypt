package xyz.meowing.krypt.features.waypoints

import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.piston.PistonHeadBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.krypt.api.location.SkyBlockIsland
import java.util.BitSet
import kotlin.math.*

/**
 * Modified code from OdinFabric,
 * Under GPL 3.0 license.
 */
object EtherWarpHelper {
    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?, val state: BlockState?) {
        companion object {
            val NONE = EtherPos(false, null, null)
        }
    }

    fun getEtherPos(position: Vec3?, distance: Double, returnEnd: Boolean = false): EtherPos {
        val player = client.player ?: return EtherPos.NONE
        val position = position ?: return EtherPos.NONE

        val eyeHeight = when {
            !player.isCrouching -> 1.62
            SkyBlockIsland.GALATEA.inIsland() -> 1.27
            else -> 1.54
        }

        val start = position.add(0.0, eyeHeight, 0.0)
        val end = player.lookAngle?.multiply(distance, distance, distance)?.add(start) ?: return EtherPos.NONE

        return traverseVoxels(start, end).takeUnless { it == EtherPos.NONE && returnEnd }
            ?: EtherPos(true, BlockPos.containing(end), null)
    }

    private fun traverseVoxels(start: Vec3, end: Vec3): EtherPos {
        var x = floor(start.x)
        var y = floor(start.y)
        var z = floor(start.z)

        val endX = floor(end.x)
        val endY = floor(end.y)
        val endZ = floor(end.z)

        val dir = end.subtract(start)
        val stepX = sign(dir.x).toInt()
        val stepY = sign(dir.y).toInt()
        val stepZ = sign(dir.z).toInt()

        val invDirX = if (dir.x != 0.0) 1.0 / dir.x else Double.MAX_VALUE
        val invDirY = if (dir.y != 0.0) 1.0 / dir.y else Double.MAX_VALUE
        val invDirZ = if (dir.z != 0.0) 1.0 / dir.z else Double.MAX_VALUE

        val tDeltaX = abs(invDirX * stepX)
        val tDeltaY = abs(invDirY * stepY)
        val tDeltaZ = abs(invDirZ * stepZ)

        var tMaxX = abs((x + max(stepX, 0) - start.x) * invDirX)
        var tMaxY = abs((y + max(stepY, 0) - start.y) * invDirY)
        var tMaxZ = abs((z + max(stepZ, 0) - start.z) * invDirZ)

        repeat(1000) {
            val pos = BlockPos(x.toInt(), y.toInt(), z.toInt())
            val chunk = client.level?.getChunk(
                SectionPos.blockToSectionCoord(pos.x),
                SectionPos.blockToSectionCoord(pos.z)
            ) ?: return EtherPos.NONE

            val blockState = chunk.getBlockState(pos)
            val blockId = Block.getId(blockState.block.defaultBlockState())

            if (blockId != 0 && !validEtherwarpBlocks.get(blockId)) {
                val feetPos = pos.above()
                val headPos = pos.above(2)

                val feetId = Block.getId(chunk.getBlockState(feetPos).block.defaultBlockState())
                val headId = Block.getId(chunk.getBlockState(headPos).block.defaultBlockState())

                if (
                    !validEtherwarpBlocks.get(feetId) ||
                    !validEtherwarpBlocks.get(headId)
                    ) return EtherPos(false, pos, blockState)

                return EtherPos(true, pos, blockState)
            }

            if (x == endX && y == endY && z == endZ) return EtherPos.NONE

            when {
                tMaxX <= tMaxY && tMaxX <= tMaxZ -> { tMaxX += tDeltaX; x += stepX }
                tMaxY <= tMaxZ -> { tMaxY += tDeltaY; y += stepY }
                else -> { tMaxZ += tDeltaZ; z += stepZ }
            }
        }

        return EtherPos.NONE
    }

    private val validEtherwarpBlocks = BitSet().apply {
        val validTypes = setOf(
            AirBlock::class, ButtonBlock::class, CarpetBlock::class, SkullBlock::class,
            WallSkullBlock::class, LadderBlock::class, SaplingBlock::class, FlowerBlock::class,
            StemBlock::class, CropBlock::class, RailBlock::class, SnowLayerBlock::class,
            TripWireBlock::class, TripWireHookBlock::class, FireBlock::class, TorchBlock::class,
            FlowerPotBlock::class, TallFlowerBlock::class, TallGrassBlock::class, BushBlock::class,
            SeagrassBlock::class, TallSeagrassBlock::class, SugarCaneBlock::class, LiquidBlock::class,
            VineBlock::class, MushroomBlock::class, PistonHeadBlock::class, WoolCarpetBlock::class,
            WebBlock::class, SmallDripleafBlock::class, LeverBlock::class, NetherWartBlock::class,
            NetherPortalBlock::class, RedStoneWireBlock::class, ComparatorBlock::class,
            RedstoneTorchBlock::class, RepeaterBlock::class
        )

        BuiltInRegistries.BLOCK.forEach { block ->
            if (validTypes.any { it.isInstance(block) }) {
                set(Block.getId(block.defaultBlockState()))
            }
        }
    }
}