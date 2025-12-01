package xyz.meowing.krypt.config

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.KnitClipboard
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.krypt.Krypt
import xyz.meowing.krypt.annotations.Command
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.features.waypoints.RoomWaypointHandler
import xyz.meowing.krypt.features.waypoints.RouteRecorder
import xyz.meowing.krypt.features.waypoints.WaypointDecoder
import xyz.meowing.krypt.features.waypoints.WaypointRegistry
import xyz.meowing.krypt.hud.HUDEditor
import xyz.meowing.krypt.managers.config.ConfigManager.configUI
import xyz.meowing.krypt.managers.config.ConfigManager.openConfig
import xyz.meowing.krypt.utils.modMessage

@Command
object ConfigCommand : Commodore("krypt") {
    init {
        literal("updateConfig") {
            runs { configName: String, newValue: String, silent: Boolean ->
                try {
                    val value = parseValue(newValue)
                    configUI.updateConfig(configName, value)
                    if (!silent) KnitChat.modMessage("§fUpdated config §b$configName §fto §b$value§f.")
                    Krypt.LOGGER.info("Updated config $configName to value $value [${value.javaClass}]")
                } catch (e: Exception) {
                    Krypt.LOGGER.error("Caught exception in command \"/krypt updateConfig\": $e")
                }
            }
        }

        literal("hud") {
            runs {
                TickScheduler.Client.post {
                    client.execute { client.setScreen(HUDEditor()) }
                }
            }
        }

        literal("modLoaded") {
            runs {
                Krypt.sendModLoaded = !Krypt.sendModLoaded
                KnitChat.fakeMessage("${if (Krypt.sendModLoaded) "§aEnabled" else "§cDisabled"} mod load messages!")
                Krypt.saveData.forceSave()
            }
        }

        literal("currentRoom") {
            runs {
                Krypt.LOGGER.info(DungeonAPI.currentRoom)
            }
        }

        literal("import") {
            runs {
                importWaypoints()
            }
        }

        literal("export") {
            runs {
                exportWaypoints()
            }
        }

        literal("dw") {
            literal("import") {
                runs {
                    importWaypoints()
                }
            }

            literal("export") {
                runs {
                    exportWaypoints()
                }
            }

            literal("start") {
                runs {
                    RouteRecorder.startRecording()
                }
            }

            literal("stop") {
                runs {
                    RouteRecorder.stopRecording()
                }
            }

            literal("reload") {
                runs {
                    WaypointRegistry.reloadFromLocal(notifyUser = true)
                    RoomWaypointHandler.reloadCurrentRoom()
                }
            }
        }

        runs {
            openConfig()
        }
    }

    private fun parseValue(input: String): Any {
        return when {
            input.toBooleanStrictOrNull() != null -> input.toBoolean()
            input.toIntOrNull() != null -> input.toInt()
            input.toDoubleOrNull() != null -> input.toDouble()
            input.toFloatOrNull() != null -> input.toFloat()
            else -> input
        }
    }

    private fun importWaypoints() {
        try {
            val clipboard = KnitClipboard.string

            KnitChat.modMessage("§eImporting waypoints from clipboard...")

            if (WaypointDecoder.importFromBase64(clipboard.trim())) {
                KnitChat.modMessage("§aSuccessfully imported waypoints!")
            } else {
                KnitChat.modMessage("§cFailed to import waypoints. Check logs for details.")
            }
        } catch (e: Exception) {
            KnitChat.modMessage("§cError reading clipboard: ${e.message}")
            Krypt.LOGGER.error("Clipboard import failed: $e")
        }
    }

    private fun exportWaypoints() {
        try {
            val encoded = WaypointDecoder.exportToBase64()
            if (encoded != null) {
                KnitClipboard.string = encoded
                KnitChat.modMessage("§aWaypoints exported to clipboard!")
            } else {
                KnitChat.modMessage("§cFailed to export waypoints. Check logs for details.")
            }
        } catch (e: Exception) {
            KnitChat.modMessage("§cError exporting waypoints: ${e.message}")
            Krypt.LOGGER.error("Export failed: $e")
        }
    }
}