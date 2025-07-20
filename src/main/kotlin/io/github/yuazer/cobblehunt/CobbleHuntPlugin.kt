package io.github.yuazer.cobblehunt

import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.listen.CobbleEventsHandler
import io.github.yuazer.cobblehunt.listen.FabricEventsHandler
import taboolib.common.platform.Plugin
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile
import taboolib.module.lang.Language

object CobbleHunt : Plugin() {
    @Config("config.yml")
    lateinit var config: ConfigFile

    @Config("cacheTripleKey.yml")
    lateinit var cacheTripleKey: ConfigFile

    @Config("cacheStringList.yml")
    lateinit var cacheStringList: ConfigFile

    override fun onEnable() {
        DataLoader.reload()
        FabricEventsHandler.register()
        CobbleEventsHandler.register()
        Language.enableFileWatcher = true
    }
}
