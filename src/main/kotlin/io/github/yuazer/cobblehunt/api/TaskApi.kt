package io.github.yuazer.cobblehunt.api

import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.model.HuntTask
import io.github.yuazer.cobblehunt.utils.ScriptUtils
import org.bukkit.Bukkit

object TaskApi {

    const val DEFAULT_PROGRESS_KEY = "default"
    const val CAPTURE_PROGRESS_PREFIX_KEY = "capture"
    const val BEAT_PROGRESS_PREFIX_KEY = "beat"

    /**
     * 添加任务：初始化所有进度
     */
    fun addTask(player: String, taskName: String): Boolean {
        val task = DataLoader.taskMap[taskName] ?: return false
        if (DataLoader.playerTaskingMap[player].contains(taskName)) return false
        DataLoader.playerTaskingMap.add(player, taskName)
        // 初始化该任务所有 countCondition 进度为 0
        task.countConditions.keys.forEach { progressKey ->
            DataLoader.taskCountMap[player, taskName, progressKey] = 0
        }
        return true
    }

    fun removeTask(player: String, taskName: String): Boolean {
        val removed = DataLoader.playerTaskingMap.removeValue(player, taskName)
        DataLoader.taskCountMap.keys()
            .filter { it.first == player && it.second == taskName }
            .forEach { (p, t, k) -> DataLoader.taskCountMap.remove(p, t, k) }
        return removed
    }

    fun getPlayerTasks(player: String): List<String> =
        DataLoader.playerTaskingMap[player]

    fun hasTask(player: String, taskName: String): Boolean =
        DataLoader.playerTaskingMap[player].contains(taskName)

    fun getTask(taskName: String): HuntTask? =
        DataLoader.taskMap[taskName]

    fun getAllTasks(): Collection<HuntTask> =
        DataLoader.taskMap.values

    /**
     * 提交任务。判定 submitConditions 是否全部通过，自动带入任务所有进度为变量
     */
    fun submitTask(player: String, taskName: String): Boolean {
        val task = getTask(taskName) ?: return false
        // 构建变量map，形如 "%捕捉闪光阿勃梭鲁%" -> 进度
        val progressMap = getPlayerTaskAllProgress(player, taskName)
            .mapKeys { "%${it.key}%" }
        val result = ScriptUtils.evalListToBoolean(task.submitConditions, progressMap)
        if (result) {
            val bukkitPlayer = Bukkit.getPlayer(player)
            if (bukkitPlayer != null) {
                task.runRewards(bukkitPlayer)
            }
            removeTask(player, taskName)
            return true
        } else {
            // 可选：失败时清零
//            task.countConditions.keys.forEach { progressKey ->
//                setTaskProgress(player, taskName, progressKey, 0)
//            }
            return false
        }
    }

    // -------- 三键进度相关 --------
    fun getTaskProgress(player: String, taskName: String, progressKey: String): Int =
        DataLoader.taskCountMap[player, taskName, progressKey] ?: 0

    fun setTaskProgress(player: String, taskName: String, progressKey: String, progress: Int) {
        DataLoader.taskCountMap[player, taskName, progressKey] = progress
    }

    fun addTaskProgress(player: String, taskName: String, progressKey: String, amount: Int = 1): Int {
        val cur = getTaskProgress(player, taskName, progressKey)
        val next = cur + amount
        setTaskProgress(player, taskName, progressKey, next)
        return next
    }

    fun subtractTaskProgress(player: String, taskName: String, progressKey: String, amount: Int = 1): Int {
        val cur = getTaskProgress(player, taskName, progressKey)
        val next = (cur - amount).coerceAtLeast(0)
        setTaskProgress(player, taskName, progressKey, next)
        return next
    }

    /**
     * 获取指定玩家所有任务及所有进度key的进度Map
     */
    fun getPlayerTaskProgressMap(player: String): Map<String, Map<String, Int>> {
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

    fun clearPlayerTasks(player: String) {
        DataLoader.playerTaskingMap.remove(player)
        DataLoader.taskCountMap.keys()
            .filter { it.first == player }
            .forEach { (p, t, k) -> DataLoader.taskCountMap.remove(p, t, k) }
    }

    fun getAllPlayersTasks(): Map<String, List<String>> =
        DataLoader.playerTaskingMap.keys().associateWith { DataLoader.playerTaskingMap[it] }

    fun getPlayersByTask(taskName: String): List<String> =
        DataLoader.playerTaskingMap.keys().filter { hasTask(it, taskName) }

    fun totalTaskingCount(): Int =
        DataLoader.playerTaskingMap.keys().sumOf { DataLoader.playerTaskingMap[it].size }

    // 获取玩家某任务所有进度Map（如 {捕捉闪光阿勃梭鲁=3, 击败20级以上宝可梦=5}）
    fun getPlayerTaskAllProgress(player: String, taskName: String): Map<String, Int> {
        val relevant = DataLoader.taskCountMap.keys()
            .filter { it.first == player && it.second == taskName }
        return relevant.associate { (_, _, progressKey) ->
            progressKey to (DataLoader.taskCountMap[player, taskName, progressKey] ?: 0)
        }
    }

}
