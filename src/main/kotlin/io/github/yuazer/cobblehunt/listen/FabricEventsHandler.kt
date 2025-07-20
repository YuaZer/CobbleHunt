package io.github.yuazer.cobblehunt.listen

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.saveToYaml
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents

object FabricEventsHandler {
    fun register() {
        ServerLifecycleEvents.SERVER_STARTED.register {
            CobbleHunt.cacheTripleKey.file?.let { DataLoader.taskCountMap.loadFromYaml(it) }
            CobbleHunt.cacheStringList.file?.let { DataLoader.playerTaskingMap.loadFromYaml(it) }
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            CobbleHunt.cacheTripleKey.file?.let { DataLoader.taskCountMap.saveToYaml(it) }
            CobbleHunt.cacheStringList.file?.let { DataLoader.playerTaskingMap.saveToYaml(it) }
        }
    }
}