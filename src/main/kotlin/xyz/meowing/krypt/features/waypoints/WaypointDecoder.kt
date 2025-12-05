package xyz.meowing.krypt.features.waypoints

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import xyz.meowing.krypt.Krypt
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64

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

    fun exportToBase64(): String? {
        return try {
            val waypointFile = WaypointFile(WaypointRegistry.getAllWaypoints())
            val json = gson.toJson(waypointFile)
            Base64.encode(compress(json))
        } catch (e: Exception) {
            e.printStackTrace()
            Krypt.LOGGER.error("Error exporting waypoints: ${e.message}")
            null
        }
    }

    private fun compress(input: String): ByteArray {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            GZIPOutputStream(byteArrayOutputStream).use { gzipOutputStream ->
                gzipOutputStream.write(input.toByteArray())
            }
            return byteArrayOutputStream.toByteArray()
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