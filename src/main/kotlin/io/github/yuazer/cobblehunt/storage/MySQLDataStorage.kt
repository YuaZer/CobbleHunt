package io.github.yuazer.cobblehunt.storage

import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.data.PlayerRotateData
import io.github.yuazer.cobblehunt.database.DatabaseManager
import io.github.yuazer.cobblehunt.database.DatabaseTables
import io.github.yuazer.cobblehunt.database.DatabaseTables.withPrefix
import io.github.yuazer.cobblehunt.enums.TaskStatus
import taboolib.common.platform.function.warning
import java.sql.SQLException

/**
 * MySQL 数据库存储实现
 */
class MySQLDataStorage : DataStorage {

    private val prefix: String
        get() = DatabaseManager.getTablePrefix()

    override fun initialize(): Boolean {
        return DatabaseManager.initialize()
    }

    override fun close() {
        DatabaseManager.close()
    }

    // ==================== 任务进度相关 ====================

    override fun setTaskProgress(player: String, taskName: String, progressKey: String, value: Int) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.INSERT_TASK_PROGRESS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.setString(3, progressKey)
                    stmt.setInt(4, value)
                    stmt.executeUpdate()
                }
            }
            // 同时更新内存
            DataLoader.taskCountMap[player, taskName, progressKey] = value
        } catch (e: SQLException) {
            warning("设置任务进度失败: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getTaskProgress(player: String, taskName: String, progressKey: String): Int? {
        // 先从内存获取
        val memoryValue = DataLoader.taskCountMap[player, taskName, progressKey]
        if (memoryValue != null) return memoryValue

        // 内存中没有，从数据库加载
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.SELECT_TASK_PROGRESS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.setString(3, progressKey)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val value = rs.getInt("progress_value")
                            DataLoader.taskCountMap[player, taskName, progressKey] = value
                            return value
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            warning("获取任务进度失败: ${e.message}")
        }
        return null
    }

    override fun deleteTaskProgress(player: String, taskName: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.DELETE_TASK_PROGRESS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.executeUpdate()
                }
            }
            // 同时删除内存
            DataLoader.taskCountMap.keys()
                .filter { it.first == player && it.second == taskName }
                .forEach { (p, t, k) -> DataLoader.taskCountMap.remove(p, t, k) }
        } catch (e: SQLException) {
            warning("删除任务进度失败: ${e.message}")
        }
    }

    override fun loadPlayerTaskProgress(player: String): Map<String, Map<String, Int>> {
        val result = mutableMapOf<String, MutableMap<String, Int>>()
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.SELECT_PLAYER_TASK_PROGRESS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val taskName = rs.getString("task_name")
                            val progressKey = rs.getString("progress_key")
                            val progressValue = rs.getInt("progress_value")

                            result.getOrPut(taskName) { mutableMapOf() }[progressKey] = progressValue
                            // 加载到内存
                            DataLoader.taskCountMap[player, taskName, progressKey] = progressValue
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            warning("加载玩家任务进度失败: ${e.message}")
        }
        return result
    }

    override fun deletePlayerProgress(player: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.DELETE_PLAYER_PROGRESS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeUpdate()
                }
            }

            // 同时删除内存
            DataLoader.taskCountMap.keys()
                .filter { it.first == player }
                .forEach { (p, t, k) -> DataLoader.taskCountMap.remove(p, t, k) }
        } catch (e: SQLException) {
            warning("删除玩家进度失败: ${e.message}")
        }
    }

    // ==================== 玩家任务列表相关 ====================

    override fun addPlayerTask(player: String, taskName: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.INSERT_PLAYER_TASK.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.executeUpdate()
                }
            }
            // 同时更新内存
            DataLoader.playerTaskingMap.add(player, taskName)
        } catch (e: SQLException) {
            warning("添加玩家任务失败: ${e.message}")
        }
    }

    override fun removePlayerTask(player: String, taskName: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.DELETE_PLAYER_TASK.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.executeUpdate()
                }
            }
            // 同时删除内存
            DataLoader.playerTaskingMap.removeValue(player, taskName)
        } catch (e: SQLException) {
            warning("删除玩家任务失败: ${e.message}")
        }
    }

    override fun getPlayerTasks(player: String): List<String> {
        // 先从内存获取
        val memoryTasks = DataLoader.playerTaskingMap[player]
        if (memoryTasks.isNotEmpty()) return memoryTasks

        // 内存中没有，从数据库加载
        val tasks = mutableListOf<String>()
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.SELECT_PLAYER_TASKS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val taskName = rs.getString("task_name")
                            tasks.add(taskName)
                        }
                    }
                }
            }
            // 加载到内存
            DataLoader.playerTaskingMap.set(player, tasks)
        } catch (e: SQLException) {
            warning("获取玩家任务失败: ${e.message}")
        }
        return tasks
    }

    override fun deleteAllPlayerTasks(player: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.DELETE_ALL_PLAYER_TASKS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeUpdate()
                }
            }
            // 同时删除内存
            DataLoader.playerTaskingMap.remove(player)
        } catch (e: SQLException) {
            warning("删除玩家所有任务失败: ${e.message}")
        }
    }

    // ==================== 任务状态相关 ====================

    override fun setTaskStatus(player: String, taskName: String, status: TaskStatus) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.INSERT_TASK_STATUS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.setString(3, status.name)
                    stmt.executeUpdate()
                }
            }
            // 同时更新内存
            DataLoader.playerTaskStatusMap[player, taskName] = status
        } catch (e: SQLException) {
            warning("设置任务状态失败: ${e.message}")
        }
    }

    override fun getTaskStatus(player: String, taskName: String): TaskStatus? {
        // 先从内存获取
        val memoryStatus = DataLoader.playerTaskStatusMap[player, taskName]
        if (memoryStatus != null) return memoryStatus

        // 内存中没有，从数据库加载
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.SELECT_TASK_STATUS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val statusName = rs.getString("status")
                            val status = TaskStatus.valueOf(statusName)
                            DataLoader.playerTaskStatusMap[player, taskName] = status
                            return status
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            warning("获取任务状态失败: ${e.message}")
        }
        return null
    }

    override fun deleteTaskStatus(player: String, taskName: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.DELETE_TASK_STATUS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setString(2, taskName)
                    stmt.executeUpdate()
                }
            }
            // 同时删除内存
            DataLoader.playerTaskStatusMap.remove(player, taskName)
        } catch (e: SQLException) {
            warning("删除任务状态失败: ${e.message}")
        }
    }

    override fun loadPlayerTaskStatus(player: String): Map<String, TaskStatus> {
        val result = mutableMapOf<String, TaskStatus>()
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.SELECT_PLAYER_TASK_STATUS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val taskName = rs.getString("task_name")
                            val statusName = rs.getString("status")
                            val status = TaskStatus.valueOf(statusName)
                            result[taskName] = status
                            // 加载到内存
                            DataLoader.playerTaskStatusMap[player, taskName] = status
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            warning("加载玩家任务状态失败: ${e.message}")
        }
        return result
    }

    override fun deletePlayerStatus(player: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.DELETE_PLAYER_STATUS.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeUpdate()
                }
            }
            // 同时删除内存
            DataLoader.playerTaskStatusMap.keys()
                .filter { it.first == player }
                .forEach { (p, t) -> DataLoader.playerTaskStatusMap.remove(p, t) }
        } catch (e: SQLException) {
            warning("删除玩家状态失败: ${e.message}")
        }
    }

    // ==================== 轮换时间相关 ====================

    override fun setRotateTime(player: String, time: Long) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.INSERT_ROTATE_TIME.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.setLong(2, time)
                    stmt.executeUpdate()
                }
            }
            // 同时更新内存
            PlayerRotateData.playerRotateTime[player] = time
        } catch (e: SQLException) {
            warning("设置轮换时间失败: ${e.message}")
        }
    }

    override fun getRotateTime(player: String): Long? {
        // 先从内存获取
        val memoryTime = PlayerRotateData.playerRotateTime[player]
        if (memoryTime != null) return memoryTime

        // 内存中没有，从数据库加载
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.SELECT_ROTATE_TIME.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeQuery().use { rs ->
                        if (rs.next()) {
                            val time = rs.getLong("last_rotate_time")
                            PlayerRotateData.playerRotateTime[player] = time
                            return time
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            warning("获取轮换时间失败: ${e.message}")
        }
        return null
    }

    override fun deleteRotateTime(player: String) {
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.DELETE_ROTATE_TIME.withPrefix(prefix)).use { stmt ->
                    stmt.setString(1, player)
                    stmt.executeUpdate()
                }
            }
            // 同时删除内存
            PlayerRotateData.playerRotateTime.remove(player)
        } catch (e: SQLException) {
            warning("删除轮换时间失败: ${e.message}")
        }
    }

    override fun loadAllRotateTime(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        try {
            DatabaseManager.getConnection().use { conn ->
                conn.prepareStatement(DatabaseTables.SELECT_ALL_ROTATE_TIME.withPrefix(prefix)).use { stmt ->
                    stmt.executeQuery().use { rs ->
                        while (rs.next()) {
                            val playerName = rs.getString("player_name")
                            val time = rs.getLong("last_rotate_time")
                            result[playerName] = time
                            // 加载到内存
                            PlayerRotateData.playerRotateTime[playerName] = time
                        }
                    }
                }
            }
        } catch (e: SQLException) {
            warning("加载所有轮换时间失败: ${e.message}")
        }
        return result
    }

    // ==================== 批量操作 ====================

    override fun loadPlayerData(player: String) {
        loadPlayerTaskProgress(player)
        getPlayerTasks(player)
        loadPlayerTaskStatus(player)
        getRotateTime(player)
    }

    override fun savePlayerData(player: String) {
        // MySQL 模式下使用实时写入，不需要批量保存
        // 但为了兼容性，保留此方法
    }

    override fun loadAllData() {
        loadAllRotateTime()
        // 其他数据在玩家加入时按需加载
    }

    override fun saveAllData() {
        // MySQL 模式下使用实时写入，不需要批量保存
    }

    override fun getStorageType(): String = "MySQL"
}
