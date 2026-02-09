package io.github.yuazer.cobblehunt.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.database.DatabaseTables.withPrefix
import taboolib.common.platform.function.warning
import java.io.PrintWriter
import java.io.StringWriter
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.sql.Connection
import java.sql.SQLException
import javax.net.ssl.SSLException

/**
 * 数据库管理器
 * 负责数据库连接和表初始化
 */
object DatabaseManager {
    private var dataSource: HikariDataSource? = null
    private var tablePrefix: String = "cobblehunt_"
    private var initialized = false
    private var lastInitError: Throwable? = null

    /**
     * 初始化数据库连接
     * @return 是否初始化成功
     */
    fun initialize(): Boolean {
        var host = "localhost"
        var port = 3306
        var database = "cobblehunt"
        var username = "root"

        try {
            val config = CobbleHunt.config
            host = config.getString("storage.mysql.host", "localhost")!!
            port = config.getInt("storage.mysql.port", 3306)
            database = config.getString("storage.mysql.database", "cobblehunt")!!
            username = config.getString("storage.mysql.username", "root")!!
            val password = config.getString("storage.mysql.password", "password")!!
            tablePrefix = config.getString("storage.mysql.table-prefix", "cobblehunt_")!!
            lastInitError = null

            val hikariConfig = HikariConfig().apply {
                jdbcUrl = buildJdbcUrl(host, port, database)
                this.username = username
                this.password = password
                driverClassName = "com.mysql.cj.jdbc.Driver"

                maximumPoolSize = config.getInt("storage.mysql.pool.maximum-pool-size", 10)
                minimumIdle = config.getInt("storage.mysql.pool.minimum-idle", 2)
                connectionTimeout = config.getLong("storage.mysql.pool.connection-timeout", 30000)
                idleTimeout = config.getLong("storage.mysql.pool.idle-timeout", 600000)
                maxLifetime = config.getLong("storage.mysql.pool.max-lifetime", 1800000)
                connectionTestQuery = "SELECT 1"
                poolName = "CobbleHunt-MySQL-Pool"
            }

            dataSource = HikariDataSource(hikariConfig)

            // Validate at startup so connection errors surface immediately.
            dataSource!!.connection.use { conn ->
                if (!conn.isValid(5)) {
                    throw SQLException("数据库连接校验失败（isValid 返回 false）")
                }
            }

            initialized = true
            createTables()
            println("§a[CobbleHunt] MySQL 数据库连接成功！")
            return true
        } catch (e: Exception) {
            initialized = false
            lastInitError = e
            warning("MySQL 数据库连接失败：${diagnoseFailure(e)}")
            warning("MySQL 配置：host=$host, port=$port, database=$database, username=$username, tablePrefix=$tablePrefix")
            warning("MySQL 详细堆栈：\n${stackTraceToString(e)}")
            close()
            return false
        }
    }

    private fun buildJdbcUrl(host: String, port: Int, database: String): String {
        return "jdbc:mysql://$host:$port/$database" +
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8"
    }

    private fun createTables() {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(DatabaseTables.CREATE_TASK_PROGRESS.withPrefix(tablePrefix))
                stmt.execute(DatabaseTables.CREATE_PLAYER_TASKS.withPrefix(tablePrefix))
                stmt.execute(DatabaseTables.CREATE_TASK_STATUS.withPrefix(tablePrefix))
                stmt.execute(DatabaseTables.CREATE_ROTATE_TIME.withPrefix(tablePrefix))
            }
        }
        println("§a[CobbleHunt] 数据库表初始化完成")
    }

    fun getConnection(): Connection {
        if (!initialized || dataSource == null) {
            throw IllegalStateException("数据库未初始化")
        }
        return dataSource!!.connection
    }

    fun testConnection(): Boolean {
        return try {
            getConnection().use { conn -> conn.isValid(5) }
        } catch (_: Exception) {
            false
        }
    }

    fun getTablePrefix(): String = tablePrefix

    fun isInitialized(): Boolean = initialized

    fun getLastInitErrorSummary(): String? = lastInitError?.let { diagnoseFailure(it) }

    fun getLastInitErrorStackTrace(): String? = lastInitError?.let { stackTraceToString(it) }

    private fun diagnoseFailure(error: Throwable): String {
        val root = rootCause(error)
        val msg = root.message?.takeIf { it.isNotBlank() } ?: "无 message"
        return when (root) {
            is ClassNotFoundException -> "未找到 MySQL 驱动类（com.mysql.cj.jdbc.Driver），请确认驱动已打包到插件。原始错误：$msg"
            is UnknownHostException -> "数据库主机无法解析（host 配置错误或 DNS 问题）。原始错误：$msg"
            is ConnectException -> "无法连接到数据库端口（MySQL 未启动、防火墙拦截或端口错误）。原始错误：$msg"
            is SocketTimeoutException -> "连接数据库超时（网络不通或响应过慢）。原始错误：$msg"
            is SSLException -> "SSL 握手失败。原始错误：$msg"
            is SQLException -> {
                if (root.errorCode == 1045) {
                    "MySQL 认证失败（ErrorCode=1045）：用户名或密码错误，或该账户没有从当前主机登录权限。请检查 storage.mysql.username/password 以及 GRANT 配置。原始错误：$msg"
                } else {
                    "SQLState=${root.sqlState}, ErrorCode=${root.errorCode}, Message=$msg"
                }
            }
            else -> "${root::class.java.simpleName}: $msg"
        }
    }

    private fun rootCause(error: Throwable): Throwable {
        var current = error
        while (current.cause != null && current.cause !== current) {
            current = current.cause!!
        }
        return current
    }

    private fun stackTraceToString(error: Throwable): String {
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    fun close() {
        try {
            dataSource?.close()
            dataSource = null
            initialized = false
            println("§e[CobbleHunt] MySQL 数据库连接已关闭")
        } catch (e: Exception) {
            warning("关闭数据库连接时出错: ${e.message}")
        }
    }
}
