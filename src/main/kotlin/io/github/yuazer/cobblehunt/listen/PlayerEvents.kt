package io.github.yuazer.cobblehunt.listen

import io.github.yuazer.cobblehunt.CobbleHunt
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent

object PlayerEvents {
    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        // 使用新的存储系统加载玩家数据
        CobbleHunt.dataStorage?.loadPlayerData(player.name)
        CobbleHunt.playerRotateManager.handlePlayerJoin(player)
    }

    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        // 使用新的存储系统保存玩家数据
        CobbleHunt.dataStorage?.savePlayerData(player.name)
    }
}
