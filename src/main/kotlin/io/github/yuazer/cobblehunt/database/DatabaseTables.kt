package io.github.yuazer.cobblehunt.database

/**
 * 数据库表定义和 SQL 语句
 */
object DatabaseTables {

    // 表创建 SQL
    const val CREATE_TASK_PROGRESS = """
        CREATE TABLE IF NOT EXISTS {prefix}task_progress (
            player_name VARCHAR(36) NOT NULL,
            task_name VARCHAR(100) NOT NULL,
            progress_key VARCHAR(100) NOT NULL,
            progress_value INT NOT NULL DEFAULT 0,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (player_name, task_name, progress_key),
            INDEX idx_player (player_name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """

    const val CREATE_PLAYER_TASKS = """
        CREATE TABLE IF NOT EXISTS {prefix}player_tasks (
            player_name VARCHAR(36) NOT NULL,
            task_name VARCHAR(100) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (player_name, task_name),
            INDEX idx_player (player_name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """

    const val CREATE_TASK_STATUS = """
        CREATE TABLE IF NOT EXISTS {prefix}task_status (
            player_name VARCHAR(36) NOT NULL,
            task_name VARCHAR(100) NOT NULL,
            status VARCHAR(20) NOT NULL,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            PRIMARY KEY (player_name, task_name),
            INDEX idx_player (player_name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """

    const val CREATE_ROTATE_TIME = """
        CREATE TABLE IF NOT EXISTS {prefix}rotate_time (
            player_name VARCHAR(36) NOT NULL PRIMARY KEY,
            last_rotate_time BIGINT NOT NULL,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            INDEX idx_player (player_name)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
    """

    // 任务进度相关 SQL
    const val INSERT_TASK_PROGRESS = """
        INSERT INTO {prefix}task_progress (player_name, task_name, progress_key, progress_value)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE progress_value = VALUES(progress_value)
    """

    const val SELECT_TASK_PROGRESS = """
        SELECT progress_value FROM {prefix}task_progress
        WHERE player_name = ? AND task_name = ? AND progress_key = ?
    """

    const val SELECT_PLAYER_TASK_PROGRESS = """
        SELECT task_name, progress_key, progress_value FROM {prefix}task_progress
        WHERE player_name = ?
    """

    const val DELETE_TASK_PROGRESS = """
        DELETE FROM {prefix}task_progress
        WHERE player_name = ? AND task_name = ?
    """

    const val DELETE_PLAYER_PROGRESS = """
        DELETE FROM {prefix}task_progress
        WHERE player_name = ?
    """

    // 玩家任务列表相关 SQL
    const val INSERT_PLAYER_TASK = """
        INSERT IGNORE INTO {prefix}player_tasks (player_name, task_name)
        VALUES (?, ?)
    """

    const val SELECT_PLAYER_TASKS = """
        SELECT task_name FROM {prefix}player_tasks
        WHERE player_name = ?
    """

    const val DELETE_PLAYER_TASK = """
        DELETE FROM {prefix}player_tasks
        WHERE player_name = ? AND task_name = ?
    """

    const val DELETE_ALL_PLAYER_TASKS = """
        DELETE FROM {prefix}player_tasks
        WHERE player_name = ?
    """

    // 任务状态相关 SQL
    const val INSERT_TASK_STATUS = """
        INSERT INTO {prefix}task_status (player_name, task_name, status)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE status = VALUES(status)
    """

    const val SELECT_TASK_STATUS = """
        SELECT status FROM {prefix}task_status
        WHERE player_name = ? AND task_name = ?
    """

    const val SELECT_PLAYER_TASK_STATUS = """
        SELECT task_name, status FROM {prefix}task_status
        WHERE player_name = ?
    """

    const val DELETE_TASK_STATUS = """
        DELETE FROM {prefix}task_status
        WHERE player_name = ? AND task_name = ?
    """

    const val DELETE_PLAYER_STATUS = """
        DELETE FROM {prefix}task_status
        WHERE player_name = ?
    """

    // 轮换时间相关 SQL
    const val INSERT_ROTATE_TIME = """
        INSERT INTO {prefix}rotate_time (player_name, last_rotate_time)
        VALUES (?, ?)
        ON DUPLICATE KEY UPDATE last_rotate_time = VALUES(last_rotate_time)
    """

    const val SELECT_ROTATE_TIME = """
        SELECT last_rotate_time FROM {prefix}rotate_time
        WHERE player_name = ?
    """

    const val SELECT_ALL_ROTATE_TIME = """
        SELECT player_name, last_rotate_time FROM {prefix}rotate_time
    """

    const val DELETE_ROTATE_TIME = """
        DELETE FROM {prefix}rotate_time
        WHERE player_name = ?
    """

    /**
     * 替换 SQL 中的表名前缀
     */
    fun String.withPrefix(prefix: String): String {
        return this.replace("{prefix}", prefix)
    }
}
