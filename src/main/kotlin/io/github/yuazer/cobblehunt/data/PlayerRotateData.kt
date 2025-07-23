package io.github.yuazer.cobblehunt.data

import io.github.yuazer.cobblehunt.CobbleHunt
import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import java.util.concurrent.ConcurrentHashMap

object PlayerRotateData {
    private val file = newFile(getDataFolder(), "player_rotate_time.yml",true)
    private val config = CobbleHunt.player_rotate_time
    // 玩家名 -> 上次轮换时间戳（毫秒）
    val playerRotateTime = ConcurrentHashMap<String, Long>()

    // 加载
    fun load() {
        playerRotateTime.clear()
        config.getKeys(false).forEach { player ->
            playerRotateTime[player] = config.getLong(player)
        }
    }

    // 保存
    fun save() {
        playerRotateTime.forEach { (player, time) ->
            config[player] = time
        }
        config.saveToFile(file)
    }

    fun get(player: String): Long = playerRotateTime[player] ?: 0L
    fun set(player: String, time: Long) {
        playerRotateTime[player] = time
        config[player] = time
        config.saveToFile(file)
    }
}
