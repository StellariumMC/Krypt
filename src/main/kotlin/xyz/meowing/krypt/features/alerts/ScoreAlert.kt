package xyz.meowing.krypt.features.alerts

import xyz.meowing.knit.api.KnitChat
import xyz.meowing.krypt.annotations.Module
import xyz.meowing.krypt.features.Feature
import xyz.meowing.krypt.utils.TitleUtils

@Module
object ScoreAlert : Feature(
    "scoreAlert",
    "Score alert",
    "Announces in party chat when you hit score milestones",
    "Alerts"
) {
    private val showTitle by config.switch("Show title", true)
    private val twoSeventyMessage by config.textInput("270 score message")
    private val threeHundredMessage by config.textInput("300 score message")

    fun show270() {
        if (!config()) return

        if (showTitle) TitleUtils.showTitle("§b270!", duration = 2000)
        if (twoSeventyMessage.isNotEmpty()) KnitChat.sendCommand("pc $twoSeventyMessage")
    }

    fun show300() {
        if (!config()) return

        if (showTitle) TitleUtils.showTitle("§a300!", duration = 2000)
        if (threeHundredMessage.isNotEmpty()) KnitChat.sendCommand("pc $threeHundredMessage")
    }
}