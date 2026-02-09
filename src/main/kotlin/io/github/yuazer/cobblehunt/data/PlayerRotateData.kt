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
        CobbleHunt.dataStorage?.loadAllRotateTime()?.forEach { (player, time) ->
            playerRotateTime[player] = time
        }
    }

    // 保存
    fun save() {
        // 使用新的存储系统，不需要手动保存
        // 保留此方法以保持兼容性
    }

    fun get(player: String): Long {
        // 先从内存获取
        val memoryTime = playerRotateTime[player]
        if (memoryTime != null) return memoryTime

        // 内存中没有，从存储加载
        val storageTime = CobbleHunt.dataStorage?.getRotateTime(player)
        if (storageTime != null) {
            playerRotateTime[player] = storageTime
            return storageTime
        }

        return 0L
    }

    fun set(player: String, time: Long) {
        playerRotateTime[player] = time
        // 使用新的存储系统实时保存
        CobbleHunt.dataStorage?.setRotateTime(player, time)
    }
}
