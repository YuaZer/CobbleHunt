package io.github.yuazer.cobblehunt

import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.data.PlayerRotateData
import io.github.yuazer.cobblehunt.listen.CobbleEventsHandler
import io.github.yuazer.cobblehunt.runnable.PlayerRotateManager
import io.github.yuazer.cobblehunt.storage.DataStorage
import io.github.yuazer.cobblehunt.storage.DataStorageFactory
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.saveToYaml
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.pluginVersion
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.lang.Language
import taboolib.module.metrics.Metrics

object CobbleHunt : Plugin() {
    @Config("config.yml")
    lateinit var config: ConfigFile

    @Config("rotateGui.yml")
    lateinit var rotateGui: ConfigFile

    @Config("cacheTripleKey.yml")
    lateinit var cacheTripleKey: ConfigFile

    @Config("cacheStringList.yml")
    lateinit var cacheStringList: ConfigFile
    @Config("cacheDoubleKey.yml")
    lateinit var cacheDoubleKey: ConfigFile

    @Config("player_rotate_time.yml")
    lateinit var player_rotate_time: ConfigFile
    @Config("icons.yml")
    lateinit var icons: ConfigFile

    // 数据存储实例
    var dataStorage: DataStorage? = null

    val playerRotateManager by lazy { PlayerRotateManager(config.getInt("rotateOptions.time",480)) }
    override fun onEnable() {
        // 初始化数据存储
        dataStorage = DataStorageFactory.createStorage()
        if (dataStorage == null) {
            warning("数据存储初始化失败，插件将无法正常工作！")
            return
        }

        DataLoader.reload()
        loadData()
        CobbleEventsHandler.register()
        playerRotateManager.startAutoRotateTask()
        Language.enableFileWatcher = true
        val metrics = Metrics(26858, pluginVersion, Platform.BUKKIT)

        println("§a[CobbleHunt] 插件已启动，当前存储模式: ${dataStorage?.getStorageType()}")
    }

    override fun onDisable() {
        saveData()
        dataStorage?.close()
        println("§e[CobbleHunt] 插件已关闭")
    }

    fun loadData() {
        dataStorage?.loadAllData()
    }

    fun saveData() {
        dataStorage?.saveAllData()
    }
}
