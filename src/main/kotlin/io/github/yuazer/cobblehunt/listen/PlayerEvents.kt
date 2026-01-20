package io.github.yuazer.cobblehunt.listen

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadKeyFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadPlayerFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.saveKeyToYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.savePlayerToYaml
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent

object PlayerEvents {
    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        CobbleHunt.cacheTripleKey.file?.let {
            DataLoader.taskCountMap.loadPlayerFromYaml(player.name, it)
        }
        CobbleHunt.cacheStringList.file?.let {
            DataLoader.playerTaskingMap.loadKeyFromYaml(player.name, it)
        }
        CobbleHunt.cacheDoubleKey.file?.let {
            DataLoader.playerTaskStatusMap.loadKeyFromYaml(player.name, it)
        }
        CobbleHunt.playerRotateManager.handlePlayerJoin(player)
    }

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        CobbleHunt.cacheTripleKey.file?.let {
            DataLoader.taskCountMap.savePlayerToYaml(player.name, it)
        }
        CobbleHunt.cacheStringList.file?.let {
            DataLoader.playerTaskingMap.saveKeyToYaml(player.name, it)
        }
        CobbleHunt.cacheDoubleKey.file?.let {
            DataLoader.playerTaskStatusMap.saveKeyToYaml(player.name, it)
        }
    }
}
