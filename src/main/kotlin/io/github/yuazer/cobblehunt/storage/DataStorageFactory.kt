package io.github.yuazer.cobblehunt.storage

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.database.DatabaseManager
import taboolib.common.platform.function.warning

/**
 * 数据存储工厂类
 * 根据配置创建对应的存储实现
 */
object DataStorageFactory {

    /**
     * 创建数据存储实例
     * @return 数据存储实例，如果创建失败返回 null
     */
    fun createStorage(): DataStorage? {
        val storageType = CobbleHunt.config.getString("storage.type", "yaml")?.lowercase() ?: "yaml"
        val fallbackToYaml = CobbleHunt.config.getBoolean("storage.mysql.fallback-to-yaml", true)

        return when (storageType) {
            "mysql" -> {
                println("§e[CobbleHunt] 尝试连接 MySQL 数据库...")
                val mysqlStorage = MySQLDataStorage()
                if (mysqlStorage.initialize()) {
                    println("§a[CobbleHunt] 成功使用 MySQL 存储模式")
                    mysqlStorage
                } else {
                    warning("MySQL 数据库连接失败")
                    DatabaseManager.getLastInitErrorSummary()?.let {
                        warning("MySQL 失败原因: $it")
                    }
                    if (fallbackToYaml) {
                        println("§e[CobbleHunt] 降级使用 YML 文件存储模式")
                        val yamlStorage = YamlDataStorage()
                        if (yamlStorage.initialize()) {
                            yamlStorage
                        } else {
                            warning("YML 存储初始化失败")
                            null
                        }
                    } else {
                        warning("已禁用降级到 YML，但 MySQL 初始化失败，强制切换到 YML 以保证插件可用")
                        val yamlStorage = YamlDataStorage()
                        if (yamlStorage.initialize()) {
                            yamlStorage
                        } else {
                            warning("YML 存储初始化失败")
                            null
                        }
                    }
                }
            }
            "yaml" -> {
                val yamlStorage = YamlDataStorage()
                if (yamlStorage.initialize()) {
                    yamlStorage
                } else {
                    warning("YML 存储初始化失败")
                    null
                }
            }
            else -> {
                warning("未知的存储类型: $storageType，使用默认的 YML 存储")
                val yamlStorage = YamlDataStorage()
                if (yamlStorage.initialize()) {
                    yamlStorage
                } else {
                    warning("YML 存储初始化失败")
                    null
                }
            }
        }
    }
}
