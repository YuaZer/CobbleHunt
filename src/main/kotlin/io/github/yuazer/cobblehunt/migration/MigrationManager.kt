package io.github.yuazer.cobblehunt.migration

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.data.PlayerRotateData
import io.github.yuazer.cobblehunt.storage.MySQLDataStorage
import io.github.yuazer.cobblehunt.storage.YamlDataStorage
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.loadFromYaml
import io.github.yuazer.cobblehunt.utils.extension.MapExtension.saveToYaml
import taboolib.common.platform.function.warning
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据迁移管理器
 */
object MigrationManager {

    /**
     * 将 YML 数据迁移到 MySQL
     */
    fun migrateYamlToMySQL(): MigrationResult {
        try {
            println("§e[CobbleHunt] 开始迁移 YML 数据到 MySQL...")

            // 1. 备份 YML 文件
            val backupResult = backupYamlFiles()
            if (!backupResult) {
                return MigrationResult(false, "备份 YML 文件失败")
            }

            // 2. 创建 MySQL 存储实例
            val mysqlStorage = MySQLDataStorage()
            if (!mysqlStorage.initialize()) {
                return MigrationResult(false, "MySQL 数据库连接失败")
            }

            // 3. 从 YML 文件加载所有数据到内存
            CobbleHunt.cacheTripleKey.file?.let { DataLoader.taskCountMap.loadFromYaml(it) }
            CobbleHunt.cacheStringList.file?.let { DataLoader.playerTaskingMap.loadFromYaml(it) }
            CobbleHunt.cacheDoubleKey.file?.let { DataLoader.playerTaskStatusMap.loadFromYaml(it) }
            PlayerRotateData.load()

            // 4. 统计数据量
            val progressCount = DataLoader.taskCountMap.size
            val taskCount = DataLoader.playerTaskingMap.keys().sumOf { DataLoader.playerTaskingMap[it].size }
            val statusCount = DataLoader.playerTaskStatusMap.keys().size
            val rotateCount = PlayerRotateData.playerRotateTime.size

            println("§e[CobbleHunt] 发现数据: $progressCount 条任务进度, $taskCount 个玩家任务, $statusCount 条任务状态, $rotateCount 条轮换时间")

            // 5. 迁移任务进度
            var migratedProgress = 0
            DataLoader.taskCountMap.forEach { player, taskName, progressKey, value ->
                mysqlStorage.setTaskProgress(player, taskName, progressKey, value)
                migratedProgress++
            }
            println("§a[CobbleHunt] 已迁移 $migratedProgress 条任务进度")

            // 6. 迁移玩家任务列表
            var migratedTasks = 0
            DataLoader.playerTaskingMap.keys().forEach { player ->
                DataLoader.playerTaskingMap[player].forEach { taskName ->
                    mysqlStorage.addPlayerTask(player, taskName)
                    migratedTasks++
                }
            }
            println("§a[CobbleHunt] 已迁移 $migratedTasks 个玩家任务")

            // 7. 迁移任务状态
            var migratedStatus = 0
            DataLoader.playerTaskStatusMap.keys().forEach { (player, taskName) ->
                val status = DataLoader.playerTaskStatusMap[player, taskName]
                if (status != null) {
                    mysqlStorage.setTaskStatus(player, taskName, status)
                    migratedStatus++
                }
            }
            println("§a[CobbleHunt] 已迁移 $migratedStatus 条任务状态")

            // 8. 迁移轮换时间
            var migratedRotate = 0
            PlayerRotateData.playerRotateTime.forEach { (player, time) ->
                mysqlStorage.setRotateTime(player, time)
                migratedRotate++
            }
            println("§a[CobbleHunt] 已迁移 $migratedRotate 条轮换时间")

            // 9. 验证数据完整性
            val verifyResult = verifyMigration(mysqlStorage, progressCount, taskCount, statusCount, rotateCount)
            if (!verifyResult) {
                warning("数据验证失败，请检查数据库")
                return MigrationResult(false, "数据验证失败")
            }

            mysqlStorage.close()

            println("§a[CobbleHunt] YML 数据迁移到 MySQL 完成！")
            println("§e[CobbleHunt] 请修改 config.yml 中的 storage.type 为 'mysql' 并重启服务器")

            return MigrationResult(
                true,
                "成功迁移 $migratedProgress 条任务进度, $migratedTasks 个玩家任务, $migratedStatus 条任务状态, $migratedRotate 条轮换时间"
            )

        } catch (e: Exception) {
            warning("迁移过程中出错: ${e.message}")
            e.printStackTrace()
            return MigrationResult(false, "迁移失败: ${e.message}")
        }
    }

    /**
     * 将 MySQL 数据导出到 YML
     */
    fun migrateMySQLToYaml(): MigrationResult {
        try {
            println("§e[CobbleHunt] 开始导出 MySQL 数据到 YML...")

            // 1. 创建 MySQL 存储实例
            val mysqlStorage = MySQLDataStorage()
            if (!mysqlStorage.initialize()) {
                return MigrationResult(false, "MySQL 数据库连接失败")
            }

            // 2. 从 MySQL 加载所有数据到内存
            mysqlStorage.loadAllData()

            // 3. 统计数据量
            val progressCount = DataLoader.taskCountMap.size
            val taskCount = DataLoader.playerTaskingMap.keys().sumOf { DataLoader.playerTaskingMap[it].size }
            val statusCount = DataLoader.playerTaskStatusMap.keys().size
            val rotateCount = PlayerRotateData.playerRotateTime.size

            println("§e[CobbleHunt] 发现数据: $progressCount 条任务进度, $taskCount 个玩家任务, $statusCount 条任务状态, $rotateCount 条轮换时间")

            // 4. 备份现有 YML 文件
            val backupResult = backupYamlFiles()
            if (!backupResult) {
                return MigrationResult(false, "备份 YML 文件失败")
            }

            // 5. 保存到 YML 文件
            CobbleHunt.cacheTripleKey.file?.let { DataLoader.taskCountMap.saveToYaml(it) }
            CobbleHunt.cacheStringList.file?.let { DataLoader.playerTaskingMap.saveToYaml(it) }
            CobbleHunt.cacheDoubleKey.file?.let { DataLoader.playerTaskStatusMap.saveToYaml(it) }
            PlayerRotateData.save()

            mysqlStorage.close()

            println("§a[CobbleHunt] MySQL 数据导出到 YML 完成！")
            println("§e[CobbleHunt] 如需切换到 YML 存储，请修改 config.yml 中的 storage.type 为 'yaml' 并重启服务器")

            return MigrationResult(
                true,
                "成功导出 $progressCount 条任务进度, $taskCount 个玩家任务, $statusCount 条任务状态, $rotateCount 条轮换时间"
            )

        } catch (e: Exception) {
            warning("导出过程中出错: ${e.message}")
            e.printStackTrace()
            return MigrationResult(false, "导出失败: ${e.message}")
        }
    }

    /**
     * 备份 YML 文件
     */
    private fun backupYamlFiles(): Boolean {
        try {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss")
            val timestamp = dateFormat.format(Date())
            val backupDir = File(CobbleHunt.config.file?.parentFile, "backup_$timestamp")

            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val filesToBackup = listOf(
                CobbleHunt.cacheTripleKey.file,
                CobbleHunt.cacheStringList.file,
                CobbleHunt.cacheDoubleKey.file,
                CobbleHunt.player_rotate_time.file
            )

            filesToBackup.forEach { file ->
                if (file != null && file.exists()) {
                    val backupFile = File(backupDir, file.name)
                    file.copyTo(backupFile, overwrite = true)
                }
            }

            println("§a[CobbleHunt] YML 文件已备份到: ${backupDir.absolutePath}")
            return true

        } catch (e: Exception) {
            warning("备份 YML 文件失败: ${e.message}")
            return false
        }
    }

    /**
     * 验证迁移数据完整性
     */
    private fun verifyMigration(
        mysqlStorage: MySQLDataStorage,
        expectedProgress: Int,
        expectedTasks: Int,
        expectedStatus: Int,
        expectedRotate: Int
    ): Boolean {
        try {
            // 清空内存数据
            DataLoader.taskCountMap.clear()
            DataLoader.playerTaskingMap.clear()
            DataLoader.playerTaskStatusMap.clear()
            PlayerRotateData.playerRotateTime.clear()

            // 从 MySQL 重新加载
            mysqlStorage.loadAllData()

            // 验证数据量
            val actualProgress = DataLoader.taskCountMap.size
            val actualTasks = DataLoader.playerTaskingMap.keys().sumOf { DataLoader.playerTaskingMap[it].size }
            val actualStatus = DataLoader.playerTaskStatusMap.keys().size
            val actualRotate = PlayerRotateData.playerRotateTime.size

            println("§e[CobbleHunt] 验证数据: 任务进度 $actualProgress/$expectedProgress, 玩家任务 $actualTasks/$expectedTasks, 任务状态 $actualStatus/$expectedStatus, 轮换时间 $actualRotate/$expectedRotate")

            return actualProgress == expectedProgress &&
                    actualTasks == expectedTasks &&
                    actualStatus == expectedStatus &&
                    actualRotate == expectedRotate

        } catch (e: Exception) {
            warning("验证数据失败: ${e.message}")
            return false
        }
    }

    /**
     * 迁移结果
     */
    data class MigrationResult(
        val success: Boolean,
        val message: String
    )
}
