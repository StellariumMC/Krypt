package xyz.meowing.krypt.features.waypoints

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import xyz.meowing.krypt.Krypt
import java.util.zip.GZIPInputStream
import kotlin.io.encoding.Base64

object WaypointDecoder {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun importFromBase64(base64Data: String): Boolean {
        return try {
            val compressed = Base64.decode(base64Data)
            val json = decompress(compressed)
            val data = parseWaypointData(json)
            WaypointRegistry.importWaypoints(data)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            Krypt.LOGGER.error("Error importing waypoints: ${e.message}")
            false
        }
    }

    private fun parseWaypointData(json: String): WaypointFile {
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

    private fun decompress(compressed: ByteArray): String {
        GZIPInputStream(compressed.inputStream()).use { gzipInputStream ->
            return gzipInputStream.bufferedReader().use { it.readText() }
        }
    }
}