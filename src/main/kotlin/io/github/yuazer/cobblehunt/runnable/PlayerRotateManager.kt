package io.github.yuazer.cobblehunt.runnable

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.api.TaskApi
import io.github.yuazer.cobblehunt.data.PlayerRotateData
import io.github.yuazer.cobblehunt.enums.TaskStatus
import io.github.yuazer.cobblehunt.utils.extension.CollectionExtension.randomSample
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.platform.BukkitPlugin
import taboolib.platform.util.sendLang

class PlayerRotateManager(
    var rotateMinutes: Int // 从配置读取轮换间隔（分钟）
) {

    // 玩家尝试轮换，返回是否成功
    fun tryRotate(player: Player): Boolean {
        val now = System.currentTimeMillis()
        val last = PlayerRotateData.get(player.name)
        val intervalMillis = rotateMinutes * 60 * 1000L
        return if (now - last >= intervalMillis) {
            // 轮换到期，清除未完成任务，允许轮换
            removeIncompleteTasks(player)
            PlayerRotateData.set(player.name, now)
            rotateTask(player)
            true
        } else {
            false // 还未到轮换时间
        }
    }

    fun forceRotate(player: Player) {
        // 无视冷却，直接清除所有任务和状态
        TaskApi.clearPlayerTasks(player.name)
        // 立即设置为当前时间，模拟一次轮换
        PlayerRotateData.set(player.name, System.currentTimeMillis())
        rotateTask(player)
        // 发送提示
        player.sendLang("rotate-force-clear-tasks")
    }

    fun rotateTask(player: Player) {
        CobbleHunt.config.getConfigurationSection("rotateOptions.tasks")?.getKeys(false)?.forEach { star ->
            val taskAmount = CobbleHunt.config.getInt("rotateOptions.tasks.$star")
            // 只保留玩家还没拥有的任务
            val candidateTasks = TaskApi.getAllTasks()
                .filter { it.star == star.toInt() && !TaskApi.hasTask(player.name, it.name) }
            // 从候选任务中随机抽取
            candidateTasks.randomSample(taskAmount).forEach { task ->
                TaskApi.addTask(player.name, task.name)
            }
        }
    }

    // 清除未完成任务
    private fun removeIncompleteTasks(player: Player) {
        val unfinished = TaskApi.getPlayerTaskNamesByStatus(player.name, TaskStatus.IN_PROGRESS)
        unfinished.forEach { _ ->
            TaskApi.clearPlayerTasks(player.name)
        }
        player.sendLang("rotate-clear-tasks")
    }

    // 可选：全服定时检查到期自动轮换
    fun startAutoRotateTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(BukkitPlugin.getInstance(), kotlinx.coroutines.Runnable {
            Bukkit.getOnlinePlayers().forEach { player ->
                val now = System.currentTimeMillis()
                val last = PlayerRotateData.get(player.name)
                val intervalMillis = rotateMinutes * 60 * 1000L
                if (now - last >= intervalMillis) {
                    removeIncompleteTasks(player)
                    PlayerRotateData.set(player.name, now)
                    // 可选提示
//                    player.sendLang("rotate-auto-cleared")
                }
            }
        }, 20 * 60, 20 * 60) // 每分钟检查一次
    }
}