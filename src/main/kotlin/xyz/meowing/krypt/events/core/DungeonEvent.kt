package xyz.meowing.krypt.events.core

import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import xyz.meowing.knit.api.events.Event
import xyz.meowing.krypt.api.dungeons.enums.DungeonFloor
import xyz.meowing.krypt.api.dungeons.enums.DungeonKey
import xyz.meowing.krypt.api.dungeons.enums.DungeonPlayer
import xyz.meowing.krypt.api.dungeons.enums.map.Checkmark

sealed class DungeonEvent {
    /**
     * Posted when the dungeon starts.
     *
     * @see xyz.meowing.krypt.api.dungeons.DungeonAPI
     * @since 1.2.0
     */
    class Start(
        val floor: DungeonFloor
    ) : Event()

    class End(
        val floor: DungeonFloor
    ) : Event()

    /**
     * Posted when the player loads into a Dungeon.
     *
     * @see xyz.meowing.krypt.api.dungeons.DungeonAPI
     * @since 1.2.0
     */
    class Enter(
        val floor: DungeonFloor
    ) : Event()

    sealed class Player {
        class Death(
            val player: DungeonPlayer
        ) : Event()
    }

    sealed class Room {
        class Change(
            val old: xyz.meowing.krypt.api.dungeons.enums.map.Room,
            val new: xyz.meowing.krypt.api.dungeons.enums.map.Room
        ) : Event()

        class StateChange(
            val room: xyz.meowing.krypt.api.dungeons.enums.map.Room,
            val oldState: Checkmark,
            val newState: Checkmark,
            val roomPlayers: List<DungeonPlayer>
        ) : Event()
    }

    /**
     * Posted when a key is picked up
     *
     * @see xyz.meowing.krypt.api.dungeons.DungeonAPI
     * @since 1.2.0
     */
    class KeyPickUp(
        val key: DungeonKey
    ) : Event()

    /**
     * Posted when a Secret is found
     *
     * @see xyz.meowing.krypt.api.dungeons.DungeonAPI
     */
    sealed class Secrets {
        class Item(
            val entityId: Int
        ) : Event()

        class Bat(
            val entity: Entity
        ) : Event()

        class Chest(
            val blockState: BlockState,
            val blockPos: BlockPos
        ) : Event()

        class Essence(
            val blockEntity: BlockEntity,
            val blockPos: BlockPos,
        ) : Event()

        class Misc(
            val secretType: Type,
            val blockPos: BlockPos
        ) : Event()

        enum class Type {
            RED_SKULL,
            LEVER
        }
    }
}