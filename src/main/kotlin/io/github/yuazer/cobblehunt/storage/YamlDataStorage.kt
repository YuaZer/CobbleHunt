package io.github.yuazer.cobblehunt.storage

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.data.PlayerRotateData
import io.github.yuazer.cobblehunt.enums.TaskStatus
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadKeyFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadPlayerFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.saveKeyToYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.savePlayerToYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.saveToYaml

/**
 * YML 文件存储实现
 * 封装现有的 YML 存储逻辑
 */
class YamlDataStorage : DataStorage {

    override fun initialize(): Boolean {
        println("§a[CobbleHunt] 使用 YML 文件存储模式")
        return true
    }

    override fun close() {
        // YML 存储不需要关闭连接
    }

    // ==================== 任务进度相关 ====================

    override fun setTaskProgress(player: String, taskName: String, progressKey: String, value: Int) {
        DataLoader.taskCountMap[player, taskName, progressKey] = value
    }

    override fun getTaskProgress(player: String, taskName: String, progressKey: String): Int? {
        return DataLoader.taskCountMap[player, taskName, progressKey]
    }

    override fun deleteTaskProgress(player: String, taskName: String) {
        DataLoader.taskCountMap.keys()
            .filter { it.first == player && it.second == taskName }
            .forEach { (p, t, k) -> DataLoader.taskCountMap.remove(p, t, k) }
    }

    override fun loadPlayerTaskProgress(player: String): Map<String, Map<String, Int>> {
        CobbleHunt.cacheTripleKey.file?.let {
            DataLoader.taskCountMap.loadPlayerFromYaml(player, it)
        }

        val relevant = DataLoader.taskCountMap.keys()
            .filter { it.first == player }
        return relevant
            .groupBy({ it.second }, { it.third })
            .mapValues { (task, keys) ->
                keys.associateWith { progressKey ->
                    DataLoader.taskCountMap[player, task, progressKey] ?: 0
                }
            }
    }

    override fun deletePlayerProgress(player: String) {
        DataLoader.taskCountMap.keys()
            .filter { it.first == player }
            .forEach { (p, t, k) -> DataLoader.taskCountMap.remove(p, t, k) }
    }

    // ==================== 玩家任务列表相关 ====================

    override fun addPlayerTask(player: String, taskName: String) {
        DataLoader.playerTaskingMap.add(player, taskName)
    }

    override fun removePlayerTask(player: String, taskName: String) {
        DataLoader.playerTaskingMap.removeValue(player, taskName)
    }

    override fun getPlayerTasks(player: String): List<String> {
        return DataLoader.playerTaskingMap[player]
    }

    override fun deleteAllPlayerTasks(player: String) {
        DataLoader.playerTaskingMap.remove(player)
    }

    // ==================== 任务状态相关 ====================

    override fun setTaskStatus(player: String, taskName: String, status: TaskStatus) {
        DataLoader.playerTaskStatusMap[player, taskName] = status
    }

    override fun getTaskStatus(player: String, taskName: String): TaskStatus? {
        return DataLoader.playerTaskStatusMap[player, taskName]
    }

    override fun deleteTaskStatus(player: String, taskName: String) {
        DataLoader.playerTaskStatusMap.remove(player, taskName)
    }

    override fun loadPlayerTaskStatus(player: String): Map<String, TaskStatus> {
        CobbleHunt.cacheDoubleKey.file?.let {
            DataLoader.playerTaskStatusMap.loadKeyFromYaml(player, it)
        }

        return DataLoader.playerTaskStatusMap.keys()
            .filter { it.first == player }
            .associate { (_, taskName) ->
                taskName to (DataLoader.playerTaskStatusMap[player, taskName] ?: TaskStatus.NOT_TAKEN)
            }
    }

    override fun deletePlayerStatus(player: String) {
        DataLoader.playerTaskStatusMap.keys()
            .filter { it.first == player }
            .forEach { (p, t) -> DataLoader.playerTaskStatusMap.remove(p, t) }
    }

    // ==================== 轮换时间相关 ====================

    override fun setRotateTime(player: String, time: Long) {
        PlayerRotateData.playerRotateTime[player] = time
        CobbleHunt.player_rotate_time[player] = time
        CobbleHunt.player_rotate_time.file?.let { file ->
            CobbleHunt.player_rotate_time.saveToFile(file)
        }
    }

    override fun getRotateTime(player: String): Long? {
        return PlayerRotateData.playerRotateTime[player]
    }

    override fun deleteRotateTime(player: String) {
        PlayerRotateData.playerRotateTime.remove(player)
        CobbleHunt.player_rotate_time[player] = null
        CobbleHunt.player_rotate_time.file?.let { file ->
            CobbleHunt.player_rotate_time.saveToFile(file)
        }
    }

    override fun loadAllRotateTime(): Map<String, Long> {
        val rotateTimeMap = mutableMapOf<String, Long>()
        val config = CobbleHunt.player_rotate_time
        config.getKeys(false).forEach { player ->
            rotateTimeMap[player] = config.getLong(player)
        }
        PlayerRotateData.playerRotateTime.clear()
        PlayerRotateData.playerRotateTime.putAll(rotateTimeMap)
        return rotateTimeMap
    }

    // ==================== 批量操作 ====================

    override fun loadPlayerData(player: String) {
        CobbleHunt.cacheTripleKey.file?.let {
            DataLoader.taskCountMap.loadPlayerFromYaml(player, it)
        }
        CobbleHunt.cacheStringList.file?.let {
            DataLoader.playerTaskingMap.loadKeyFromYaml(player, it)
        }
        CobbleHunt.cacheDoubleKey.file?.let {
            DataLoader.playerTaskStatusMap.loadKeyFromYaml(player, it)
        }
    }

    override fun savePlayerData(player: String) {
        CobbleHunt.cacheTripleKey.file?.let {
            DataLoader.taskCountMap.savePlayerToYaml(player, it)
        }
        CobbleHunt.cacheStringList.file?.let {
            DataLoader.playerTaskingMap.saveKeyToYaml(player, it)
        }
        CobbleHunt.cacheDoubleKey.file?.let {
            DataLoader.playerTaskStatusMap.saveKeyToYaml(player, it)
        }
    }

    override fun loadAllData() {
        CobbleHunt.cacheTripleKey.file?.let { DataLoader.taskCountMap.loadFromYaml(it) }
        CobbleHunt.cacheStringList.file?.let { DataLoader.playerTaskingMap.loadFromYaml(it) }
        CobbleHunt.cacheDoubleKey.file?.let { DataLoader.playerTaskStatusMap.loadFromYaml(it) }
        PlayerRotateData.load()
    }

    override fun saveAllData() {
        CobbleHunt.cacheTripleKey.file?.let { DataLoader.taskCountMap.saveToYaml(it) }
        CobbleHunt.cacheStringList.file?.let { DataLoader.playerTaskingMap.saveToYaml(it) }
        CobbleHunt.cacheDoubleKey.file?.let { DataLoader.playerTaskStatusMap.saveToYaml(it) }
        PlayerRotateData.save()
    }

    override fun getStorageType(): String = "YAML"
}
