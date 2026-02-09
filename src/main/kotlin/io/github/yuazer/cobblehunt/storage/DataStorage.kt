package io.github.yuazer.cobblehunt.storage

import io.github.yuazer.cobblehunt.enums.TaskStatus

/**
 * 数据存储接口
 * 定义所有数据存储操作
 */
interface DataStorage {

    /**
     * 初始化存储
     * @return 是否成功初始化
     */
    fun initialize(): Boolean

    /**
     * 关闭存储连接
     */
    fun close()

    // ==================== 任务进度相关 ====================

    /**
     * 设置任务进度
     */
    fun setTaskProgress(player: String, taskName: String, progressKey: String, value: Int)

    /**
     * 获取任务进度
     */
    fun getTaskProgress(player: String, taskName: String, progressKey: String): Int?

    /**
     * 删除任务的所有进度
     */
    fun deleteTaskProgress(player: String, taskName: String)

    /**
     * 加载玩家的所有任务进度
     * @return Map<任务名, Map<进度键, 进度值>>
     */
    fun loadPlayerTaskProgress(player: String): Map<String, Map<String, Int>>

    /**
     * 删除玩家的所有任务进度
     */
    fun deletePlayerProgress(player: String)

    // ==================== 玩家任务列表相关 ====================

    /**
     * 添加玩家任务
     */
    fun addPlayerTask(player: String, taskName: String)

    /**
     * 删除玩家任务
     */
    fun removePlayerTask(player: String, taskName: String)

    /**
     * 获取玩家的所有任务
     */
    fun getPlayerTasks(player: String): List<String>

    /**
     * 删除玩家的所有任务
     */
    fun deleteAllPlayerTasks(player: String)

    // ==================== 任务状态相关 ====================

    /**
     * 设置任务状态
     */
    fun setTaskStatus(player: String, taskName: String, status: TaskStatus)

    /**
     * 获取任务状态
     */
    fun getTaskStatus(player: String, taskName: String): TaskStatus?

    /**
     * 删除任务状态
     */
    fun deleteTaskStatus(player: String, taskName: String)

    /**
     * 加载玩家的所有任务状态
     * @return Map<任务名, 状态>
     */
    fun loadPlayerTaskStatus(player: String): Map<String, TaskStatus>

    /**
     * 删除玩家的所有任务状态
     */
    fun deletePlayerStatus(player: String)

    // ==================== 轮换时间相关 ====================

    /**
     * 设置玩家轮换时间
     */
    fun setRotateTime(player: String, time: Long)

    /**
     * 获取玩家轮换时间
     */
    fun getRotateTime(player: String): Long?

    /**
     * 删除玩家轮换时间
     */
    fun deleteRotateTime(player: String)

    /**
     * 加载所有玩家的轮换时间
     * @return Map<玩家名, 时间戳>
     */
    fun loadAllRotateTime(): Map<String, Long>

    // ==================== 批量操作 ====================

    /**
     * 加载玩家的所有数据到内存
     */
    fun loadPlayerData(player: String)

    /**
     * 保存玩家的所有数据
     */
    fun savePlayerData(player: String)

    /**
     * 加载所有数据到内存
     */
    fun loadAllData()

    /**
     * 保存所有数据
     */
    fun saveAllData()

    /**
     * 获取存储类型名称
     */
    fun getStorageType(): String
}
