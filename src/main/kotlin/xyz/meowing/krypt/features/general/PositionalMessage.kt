package xyz.meowing.krypt.features.general

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.util.CommonColors
import net.minecraft.world.phys.Vec3
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.command.utils.GreedyString
import xyz.meowing.krypt.annotations.Command
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.api.data.StoredFile
import xyz.meowing.krypt.api.location.SkyBlockIsland
import xyz.meowing.krypt.events.core.RenderEvent
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.rendering.Render3D
import xyz.meowing.knit.api.KnitPlayer.player
import xyz.meowing.krypt.api.dungeons.DungeonAPI
import xyz.meowing.krypt.events.core.TickEvent

@Module
object PositionalMessage : Feature(
    "positionalMsg",
    "Positional message",
    "Use \"/krypt pos help\" for more info",
    "General",
    island = SkyBlockIsland.THE_CATACOMBS
) {
    private data class PosMsg(val pos: Vec3, val radius: Float, val message: String, var sentMessage: Boolean = false) {
        val radiusSquared: Float
            get() = radius * radius
    }

    private val configCache: HashMap<String, MutableList<PosMsg>> = HashMap<String, MutableList<PosMsg>>()
    private var posMsgList: MutableList<PosMsg> = mutableListOf<PosMsg>()
    private val posMsgData = StoredFile("features/PosMsg")
    private var currentConfig: String = "default"
    var jsonObject by posMsgData.jsonObject("posMsgData")

    override fun initialize() {
        if (!jsonObject.has("config")) jsonObject.addProperty("config", "default")
        switchConfig(jsonObject.get("config").asString)

        register<RenderEvent.World.Last> { event ->
            if (!DungeonAPI.inBoss) return@register

            posMsgList.forEach { data ->
                Render3D.drawFilledCircle(
                    consumers = event.context.consumers(),
                    matrixStack = event.context.matrixStack(),
                    center = data.pos,
                    radius = data.radius,
                    segments = 256,
                    borderColor = CommonColors.WHITE,
                    fillColor = CommonColors.RED
                )
            }
        }

        register<TickEvent.Client> { _ ->
            if (!DungeonAPI.inBoss) return@register
            val playerPos = player?.position() ?: return@register

            for (entry in posMsgList) {
                if (isInsideCircle(playerPos, entry.pos, entry.radiusSquared)) {
                    if (!entry.sentMessage) {
                        KnitChat.sendMessage("/pc " + entry.message)
                        entry.sentMessage = true
                    }
                } else {
                    entry.sentMessage = false
                }
            }
        }
    }

    fun addPos(pos: Vec3, radius: Float, message: String) {
        KnitChat.fakeMessage("Added a new position!")
        posMsgList.add(PosMsg(pos.round(), radius, message))
    }

    fun addPos(radius: Float, message: String) {
        val player = player ?: return
        addPos(player.position(), radius, message)
    }

    fun removePos(index: Int) {
        if (index !in posMsgList.indices) {
            KnitChat.fakeMessage("Index exceeds current list size: ${posMsgList.size}")
            return
        }

        KnitChat.fakeMessage("Removed the $index entry")
        posMsgList.removeAt(index)
    }

    fun save() {
        KnitChat.fakeMessage("Saved config!")
        val jsonArray = JsonArray()
        posMsgList.forEach { element ->
            jsonArray.add(createJsonObject(element))
        }
        jsonObject.add(currentConfig, jsonArray)

        jsonObject.addProperty("config", currentConfig)
        posMsgData.forceSave()
    }

    fun saveConfig() {
        jsonObject.addProperty("config", currentConfig)
        posMsgData.forceSave()
    }

    fun switchConfig(newConfig: String) {
        currentConfig = newConfig
        saveConfig()
        KnitChat.fakeMessage("Switched to $newConfig!")

        configCache[newConfig]?.let {
            posMsgList = it
            return
        }

        posMsgList = mutableListOf()

        if (!jsonObject.has(newConfig)) {
            jsonObject.add(newConfig, JsonArray())
            configCache[newConfig] = posMsgList
            return
        }

        jsonObject.get(newConfig).asJsonArray.forEach { element ->
            posMsgList.add(getDataClass(element.asJsonObject))
        }

        configCache[newConfig] = posMsgList
    }

    fun listEntries() {
        if (posMsgList.isEmpty()) {
            KnitChat.fakeMessage("No entries in current config.")
            return
        }
        posMsgList.forEachIndexed { i, entry ->
            KnitChat.fakeMessage("$i: x: ${entry.pos.x} y: ${entry.pos.y} z: ${entry.pos.z} radius: ${entry.radius} message: ${entry.message}")
        }
    }

    fun listConfigs() {
        jsonObject.entrySet().forEach { entry ->
            if (entry.key != "config") {
                KnitChat.fakeMessage(entry.key)
            }
        }
    }

    private fun createJsonObject(data: PosMsg): JsonObject {
        val jsonObject = JsonObject()

        jsonObject.addProperty(PosMsgProperties.X.toString(), data.pos.x)
        jsonObject.addProperty(PosMsgProperties.Y.toString(), data.pos.y)
        jsonObject.addProperty(PosMsgProperties.Z.toString(), data.pos.z)
        jsonObject.addProperty(PosMsgProperties.RADIUS.toString(), data.radius)
        jsonObject.addProperty(PosMsgProperties.MESSAGE.toString(), data.message)

        return jsonObject
    }

    private fun getDataClass(data: JsonObject): PosMsg {
        return PosMsg(
            Vec3(
                data.get((PosMsgProperties.X.toString())).asDouble,
                data.get(PosMsgProperties.Y.toString()).asDouble,
                data.get(PosMsgProperties.Z.toString()).asDouble),
            data.get(PosMsgProperties.RADIUS.toString()).asFloat,
            data.get(PosMsgProperties.MESSAGE.toString()).asString
        )
    }

    private fun Double.round(): Double = kotlin.math.round(this * 1000.0) / 1000.0
    private fun Vec3.round(): Vec3 = Vec3(x.round(), y.round(), z.round())

    private fun isInsideCircle(playerPos: Vec3, center: Vec3, radiusSquared: Float): Boolean {
        val x = playerPos.x - center.x
        val y = playerPos.y - center.y
        val z = playerPos.z - center.z
        return x * x + y * y + z * z <= radiusSquared
    }
}

@Command
object CommandHandler : Commodore("krypt") {
    init {
        literal("pos") {

            literal("help") {
                runs {
                    KnitChat.fakeMessage("List of commands: \n" +
                            "at: x y z radius message | adds a new point at x y z\n" +
                            "remove: index\n" +
                            "list: current or configs\n" +
                            "save\n" +
                            "add: radius message | adds a new point at current position\n" +
                            "switch: new config\n"
                    )
                }
            }

            literal("at") {
                runs { x: Double, y: Double, z: Double, radius: Float, message: GreedyString ->
                    PositionalMessage.addPos(Vec3(x, y, z), radius, message.string)
                }
            }

            literal("remove") {
                runs { index: Int ->
                    PositionalMessage.removePos(index)
                }
            }

            literal("list") {
                literal("current") {
                    runs {
                        PositionalMessage.listEntries()
                    }
                }

                literal("configs") {
                    runs {
                        PositionalMessage.listConfigs()
                    }
                }
            }

            literal("save") {
                runs {
                    PositionalMessage.save()
                }
            }

            literal("add") {
                runs { radius: Float, message: GreedyString ->
                    PositionalMessage.addPos(radius, message.string)
                }
            }

            literal("switch") {
                runs { newConfig: String ->
                    PositionalMessage.switchConfig(newConfig)
                }
            }
        }
    }
}

private enum class PosMsgProperties(private val s: String) {
    X("x"),
    Y("y"),
    Z("z"),
    RADIUS("radius"),
    MESSAGE("message")
    ;

    override fun toString() = s
}