package io.github.yuazer.cobblehunt.api

import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.enums.TaskStatus
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
        setTaskStatus(player, taskName, TaskStatus.IN_PROGRESS)
        return true
    }

    fun removeTask(player: String, taskName: String): Boolean {
        val removed = DataLoader.playerTaskingMap.removeValue(player, taskName)
        DataLoader.taskCountMap.keys()
            .filter { it.first == player && it.second == taskName }
            .forEach { (p, t, k) -> DataLoader.taskCountMap.remove(p, t, k) }
//        setTaskStatus(player, taskName, TaskStatus.NOT_TAKEN)
//        DataLoader.playerTaskStatusMap.remove(player, taskName)
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
            setTaskStatus(player, taskName, TaskStatus.COMPLETED)
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
        // 这里移除所有任务状态
        DataLoader.playerTaskStatusMap.keys()
            .filter { it.first == player }
            .forEach { (p, t) -> DataLoader.playerTaskStatusMap.remove(p, t) }
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
    /** 获取玩家指定星级的所有任务对象 */
    fun getPlayerTasksByStar(player: String, star: Int): List<HuntTask> =
        DataLoader.playerTaskingMap[player]
            .mapNotNull { DataLoader.taskMap[it] }
            .filter { it.star == star }

    /** 获取玩家指定星级的所有任务名 */
    fun getPlayerTaskNamesByStar(player: String, star: Int): List<String> =
        DataLoader.playerTaskingMap[player]
            .filter { DataLoader.taskMap[it]?.star == star }
    // 查询玩家某任务的状态（默认为 NOT_TAKEN）
    fun getTaskStatus(player: String, taskName: String): TaskStatus =
        DataLoader.playerTaskStatusMap[player, taskName] ?: TaskStatus.NOT_TAKEN

    // 设置玩家某任务状态
    fun setTaskStatus(player: String, taskName: String, status: TaskStatus) {
        DataLoader.playerTaskStatusMap[player, taskName] = status
        println("玩家$player 的任务 $taskName 状态已设置为 ${status.inChinese()}")
    }

    // 获取玩家所有处于某状态的任务名
    fun getPlayerTaskNamesByStatus(player: String, status: TaskStatus): List<String> =
        DataLoader.taskMap.keys.filter { getTaskStatus(player, it) == status }

    // 获取玩家所有处于某状态的任务对象
    fun getPlayerTasksByStatus(player: String, status: TaskStatus): List<HuntTask> =
        getPlayerTaskNamesByStatus(player, status).mapNotNull { getTask(it) }

}
