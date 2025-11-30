package xyz.meowing.krypt.features.waypoints

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.utils.NetworkUtils
import xyz.meowing.krypt.utils.modMessage
import java.io.File

@Module
object WaypointRegistry {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val waypointMap = mutableMapOf<String, List<WaypointData>>()
    private val localFile = File("config/krypt/waypoints.json")

    private const val REMOTE_URL = "https://raw.githubusercontent.com/StellariumMC/zen-data/refs/heads/main/assets/defaults/waypoints.json"

    init {
        if (localFile.exists()) {
            loadFromLocal()
        } else {
            Krypt.LOGGER.info("WaypointRegistry: Local file not found, fetching from remote...")
            fetchFromRemote()
        }
    }

    private fun fetchFromRemote() {
        NetworkUtils.fetchJson<WaypointFile>(
            url = REMOTE_URL,
            onSuccess = { data ->
                loadWaypointsData(data)
                saveToLocal(data)
                Krypt.LOGGER.info("WaypointRegistry: Downloaded and saved waypoints from remote")
            },
            onError = { error ->
                Krypt.LOGGER.error("WaypointRegistry: Failed to fetch waypoints - ${error.message}")
            }
        )
    }

    fun reloadFromLocal(notifyUser: Boolean = true) {
        if (notifyUser) KnitChat.modMessage("§eReloading waypoints from local file...")

        waypointMap.clear()
        RoomWaypointHandler.clear()

        if (!notifyUser) return
        val message = if (loadFromLocal()) "§aSuccessfully reloaded waypoints from local file" else "§cFailed to reload waypoints from local file"

        KnitChat.modMessage(message)
    }

    fun importWaypoints(data: WaypointFile) {
        loadWaypointsData(data)
        saveToLocal(data)
        Krypt.LOGGER.info("WaypointRegistry: Imported waypoints")
    }

    private fun loadWaypointsData(data: WaypointFile) {
        waypointMap.clear()
        waypointMap.putAll(data.rooms)
        RoomWaypointHandler.reloadCurrentRoom()
        Krypt.LOGGER.info("WaypointRegistry: Loaded ${waypointMap.size} rooms")
    }

    private fun loadFromLocal(): Boolean {
        return runCatching {
            if (!localFile.exists()) return false
            val json = localFile.readText(Charsets.UTF_8)
            val data = parseWaypointFile(json)
            loadWaypointsData(data)
            Krypt.LOGGER.info("WaypointRegistry: Loaded waypoints from local file")
            true
        }.getOrElse {
            Krypt.LOGGER.error("WaypointRegistry: Failed to load local waypoints - ${it.message}")
            false
        }
    }

    private fun parseWaypointFile(json: String): WaypointFile {
        val jsonObject = gson.fromJson(json, JsonObject::class.java)

        return if (jsonObject.has("rooms")) {
            gson.fromJson(json, WaypointFile::class.java)
        } else {
            val roomsMap: Map<String, List<WaypointData>> = gson.fromJson(
                json,
                object : TypeToken<Map<String, List<WaypointData>>>() {}.type
            )
            WaypointFile(roomsMap)
        }
    }

    private fun saveToLocal(data: WaypointFile) {
        runCatching {
            localFile.parentFile?.mkdirs()
            localFile.writeText(gson.toJson(data), Charsets.UTF_8)
            Krypt.LOGGER.info("WaypointRegistry: Saved waypoints to local file")
        }.onFailure {
            Krypt.LOGGER.error("WaypointRegistry: Failed to save waypoints locally - ${it.message}")
        }
    }

    fun getWaypointsForRoom(roomName: String): List<WaypointData>? {
        return waypointMap[roomName]
    }
}