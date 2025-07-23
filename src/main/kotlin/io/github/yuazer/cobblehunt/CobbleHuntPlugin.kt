package io.github.yuazer.cobblehunt

import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.data.PlayerRotateData
import io.github.yuazer.cobblehunt.listen.CobbleEventsHandler
import io.github.yuazer.cobblehunt.runnable.PlayerRotateManager
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.saveToYaml
import taboolib.common.platform.Plugin
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.lang.Language

object CobbleHunt : Plugin() {
    @Config("config.yml")
    lateinit var config: ConfigFile

    @Config("rotateGui.yml")
    lateinit var rotateGui: ConfigFile

    @Config("cacheTripleKey.yml")
    lateinit var cacheTripleKey: ConfigFile

    @Config("cacheStringList.yml")
    lateinit var cacheStringList: ConfigFile
    @Config("player_rotate_time.yml")
    lateinit var player_rotate_time: ConfigFile
    @Config("icons.yml")
    lateinit var icons: ConfigFile

    val playerRotateManager by lazy { PlayerRotateManager(config.getInt("rotateOptions.time",480)) }
    override fun onEnable() {
        DataLoader.reload()
        loadData()
        CobbleEventsHandler.register()
        playerRotateManager.startAutoRotateTask()
        Language.enableFileWatcher = true
    }

    override fun onDisable() {
        saveData()
    }

    fun loadData() {
        cacheTripleKey.file?.let { DataLoader.taskCountMap.loadFromYaml(it) }
        cacheStringList.file?.let { DataLoader.playerTaskingMap.loadFromYaml(it) }
        PlayerRotateData.load()
    }

    fun saveData() {
        cacheTripleKey.file?.let { DataLoader.taskCountMap.saveToYaml(it) }
        cacheStringList.file?.let { DataLoader.playerTaskingMap.saveToYaml(it) }
        PlayerRotateData.save()
    }
}
