package xyz.meowing.krypt.api.dungeons.enums

import net.minecraft.world.entity.Entity
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.krypt.api.dungeons.handlers.MapScanner
import xyz.meowing.krypt.api.dungeons.enums.map.Room
import xyz.meowing.krypt.api.dungeons.enums.map.RoomClearInfo
import xyz.meowing.krypt.api.hypixel.HypixelAPI
import java.util.UUID

class DungeonPlayer(
    val name: String,
    dungeonClass: DungeonClass? = null,
    classLevel: Int? = null,
    cataLevel: Int? = null
) {
    var iconX: Double? = null
    var iconZ: Double? = null
    var realX: Double? = null
    var realZ: Double? = null
    var yaw: Float? = null

    var deaths = 0
    var minRooms = 0
    var maxRooms = 0

    var dungeonClass: DungeonClass? = dungeonClass
        internal set

    var classLevel: Int? = classLevel
        internal set

    var cataLevel: Int? = cataLevel
        internal set

    var dead: Boolean = false
        internal set

    private var initSecrets: Int? = null
    private var currSecrets: Int? = null

    val secrets get() = (currSecrets ?: initSecrets ?: 0) - (initSecrets ?: 0)

    val entity: Entity? = KnitClient.world?.entitiesForRendering()?.find { it.name.stripped == name }

    val uuid: UUID? get() = entity?.uuid

    var inRender = false

    var currRoom: Room? = null
    var lastRoom: Room? = null

    val clearedRooms = mutableMapOf(
        "WHITE" to mutableMapOf(),
        "GREEN" to mutableMapOf<String, RoomClearInfo>()
    )

    init {
        HypixelAPI.fetchSecrets(uuid.toString(), 120_000) { secrets ->
            initSecrets = secrets
            currSecrets = secrets
        }
    }

    fun updateSecrets() {
        if (uuid == null) return

        HypixelAPI.fetchSecrets(uuid.toString(), cacheMs = 0) { secrets ->
            currSecrets = secrets
        }
    }

    fun getGreenChecks(): MutableMap<String, RoomClearInfo> = clearedRooms["GREEN"] ?: mutableMapOf()
    fun getWhiteChecks(): MutableMap<String, RoomClearInfo> = clearedRooms["WHITE"] ?: mutableMapOf()

    internal fun missingData(): Boolean = dungeonClass == null || classLevel == null || cataLevel == null
}