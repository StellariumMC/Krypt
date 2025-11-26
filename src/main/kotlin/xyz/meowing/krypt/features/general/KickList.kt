package xyz.meowing.krypt.features.general

import com.google.gson.JsonArray
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.krypt.annotations.Command
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.data.StoredFile
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.config.ui.elements.base.ElementType
import xyz.meowing.krypt.events.core.ChatEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.managers.config.ConfigElement
import xyz.meowing.krypt.managers.config.ConfigManager
import xyz.meowing.krypt.utils.modMessage

@Module
object KickList : Feature(
    "autoKick",
    island = SkyBlockIsland.DUNGEON_HUB
){

    val PARTY_JOIN_REGEX = Regex("^Party Finder > (?<name>[A-Za-z0-9_]+) joined the dungeon group! \\(.+\\)$")

    override fun addConfig() {
        ConfigManager.addFeature(
            "Auto Kick",
            "Auto kick people that are on the list",
            "General",
            ConfigElement(
                "autoKick",
                ElementType.Switch(false)
            )
        )
    }

    private val kickListJson = StoredFile("features/KickList")
    var config by kickListJson.jsonObject("players")
    val kickList: MutableList<String> = mutableListOf()

    override fun initialize() {
        if(!config.has("players")) config.add("players", JsonArray())
        config.get("players").asJsonArray.forEach { element ->
            kickList.add(element.asString)
        }

        register<ChatEvent.Receive> { event ->
            if(event.isActionBar) return@register
            val match = PARTY_JOIN_REGEX.find(event.message.stripped)
            if(match != null) {
                val name = match.groups["name"]?.value
                kick(name!!)
                println(name)
            }
        }
    }

    fun kick(name: String) {
        KnitChat.sendCommand("p kick $name")
    }

    fun addToList(name: String) {
        KnitChat.modMessage("Added $name to the kick list!")
        kickList.add(name)
        save()
    }

    fun removeFromList(name: String) {
        KnitChat.modMessage("Removed $name from the kick list!")
        kickList.remove(name)
        save()
    }

    fun removeFromList(index: Int) {
        KnitChat.modMessage("Removed ${kickList[index]} from the kick list!")
        kickList.removeAt(index)
        save()
    }

    fun listEntries() {
        KnitChat.modMessage("Current entries: ")
        for(entry in kickList) {
            KnitChat.modMessage(entry)
        }
    }

    fun save() {
        val jsonArray = JsonArray()
        kickList.forEach { entry ->
            jsonArray.add(entry)
        }

        config.add("players", jsonArray)
        kickListJson.forceSave()
    }
}

@Suppress("unused")
@Command
object KickListCommand : Commodore("krypt") {
    init {
        literal("kick") {
            literal("add") {
                runs { name: String ->
                    KickList.addToList(name)
                }
            }
            literal("remove") {
                runs { name: String ->
                    KickList.removeFromList(name)
                }

                runs { index: Int ->
                    KickList.removeFromList(index)
                }
            }
            literal("list") {
                runs {
                    KickList.listEntries()
                }
            }
        }
    }
}