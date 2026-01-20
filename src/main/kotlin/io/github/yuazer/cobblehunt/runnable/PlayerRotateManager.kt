package io.github.yuazer.cobblehunt.runnable

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.api.TaskApi
import io.github.yuazer.cobblehunt.data.PlayerRotateData
import io.github.yuazer.cobblehunt.enums.TaskStatus
import io.github.yuazer.cobblehunt.utils.extension.CollectionExtension.randomSample
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import taboolib.platform.BukkitPlugin
import taboolib.platform.util.sendLang

class PlayerRotateManager(
    var rotateMinutes: Int // ä»Žé…ç½®è¯»å–è½®æ¢é—´éš”ï¼ˆåˆ†é’Ÿï¼?
) {
    private var autoTask: BukkitTask? = null

    fun reloadOptions() {
        rotateMinutes = CobbleHunt.config.getInt("rotateOptions.time", 480)
        startAutoRotateTask()
    }

    private fun rotationEnabled(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.enabled", true)

    private fun clearOnRotate(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.clearOnRotate", true)

    private fun clearOnlyInProgress(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.clearOnlyInProgress", true)

    private fun fillMissingOnly(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.fillMissingOnly", true)

    private fun allowRepeatCompleted(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.allowRepeatCompleted", true)

    private fun autoRotateEnabled(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.auto.enabled", true)

    private fun autoRotateNotify(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.auto.notify", false)

    private fun autoRotateIfNoRecord(): Boolean =
        CobbleHunt.config.getBoolean("rotateOptions.auto.rotateIfNoRecord", true)

    // çŽ©å®¶å°è¯•è½®æ¢ï¼Œè¿”å›žæ˜¯å¦æˆåŠ?
    fun tryRotate(player: Player): Boolean {
        if (!rotationEnabled()) return false
        val now = System.currentTimeMillis()
        val last = PlayerRotateData.get(player.name)
        val intervalMillis = rotateMinutes.coerceAtLeast(1) * 60 * 1000L
        val shouldRotate = now - last >= intervalMillis
        return if (shouldRotate) {
            rotateNow(player, now, notifyClear = true, notifyRotate = false)
            true
        } else {
            player.sendLang("time-not-to-rotate")
            false // è¿˜æœªåˆ°è½®æ¢æ—¶é—?
        }
    }

    fun forceRotate(player: Player) {
        // æ— è§†å†·å´ï¼Œç›´æŽ¥æ¸…é™¤æ‰€æœ‰ä»»åŠ¡å’ŒçŠ¶æ€?
        TaskApi.clearPlayerTasks(player.name)
        // ç«‹å³è®¾ç½®ä¸ºå½“å‰æ—¶é—´ï¼Œæ¨¡æ‹Ÿä¸€æ¬¡è½®æ?
        PlayerRotateData.set(player.name, System.currentTimeMillis())
        rotateTask(player)
        // å‘é€æç¤?
        player.sendLang("rotate-force-clear-tasks")
    }

    fun rotateTask(player: Player) {
        val fillMissing = fillMissingOnly()
        val allowRepeat = allowRepeatCompleted()
        CobbleHunt.config.getConfigurationSection("rotateOptions.tasks")?.getKeys(false)?.forEach { starKey ->
            val star = starKey.toIntOrNull() ?: return@forEach
            val taskAmount = CobbleHunt.config.getInt("rotateOptions.tasks.$starKey")
            val currentTasks = TaskApi.getPlayerTaskNamesByStar(player.name, star)
            val toAdd = if (fillMissing) {
                (taskAmount - currentTasks.size).coerceAtLeast(0)
            } else {
                taskAmount
            }
            if (toAdd <= 0) return@forEach
            // åªä¿ç•™çŽ©å®¶è¿˜æ²¡æ‹¥æœ‰çš„ä»»åŠ¡
            val candidateTasks = TaskApi.getAllTasks()
                .filter { it.star == star && !TaskApi.hasTask(player.name, it.name) }
                .filter { allowRepeat || TaskApi.getTaskStatus(player.name, it.name) != TaskStatus.COMPLETED }
            // ä»Žå€™é€‰ä»»åŠ¡ä¸­éšæœºæŠ½å–
            candidateTasks.randomSample(toAdd).forEach { task ->
                TaskApi.addTask(player.name, task.name)
            }
        }
    }

    // æ¸…é™¤æœªå®Œæˆä»»åŠ?
    private fun removeIncompleteTasks(player: Player, notify: Boolean) {
        if (clearOnlyInProgress()) {
            val inProgress = TaskApi.getPlayerTasks(player.name)
                .filter { TaskApi.getTaskStatus(player.name, it) == TaskStatus.IN_PROGRESS }
            inProgress.forEach { taskName ->
                TaskApi.removeTask(player.name, taskName)
                TaskApi.setTaskStatus(player.name, taskName, TaskStatus.NOT_TAKEN)
            }
        } else {
            TaskApi.clearPlayerTasks(player.name)
        }
        if (notify) {
            player.sendLang("rotate-clear-tasks")
        }
    }

    fun handlePlayerJoin(player: Player) {
        if (!rotationEnabled()) return
        if (!CobbleHunt.config.getBoolean("rotateOptions.onJoin.enabled", true)) return
        val onlyIfNoTasks = CobbleHunt.config.getBoolean("rotateOptions.onJoin.onlyIfNoTasks", true)
        val onlyIfNoRecord = CobbleHunt.config.getBoolean("rotateOptions.onJoin.onlyIfNoRecord", true)
        if (onlyIfNoTasks && TaskApi.getPlayerTasks(player.name).isNotEmpty()) return
        if (onlyIfNoRecord && PlayerRotateData.get(player.name) != 0L) return
        rotateTask(player)
        PlayerRotateData.set(player.name, System.currentTimeMillis())
        if (CobbleHunt.config.getBoolean("rotateOptions.onJoin.notify", false)) {
            player.sendLang("rotate-auto-rotated")
        }
    }

    private fun rotateNow(player: Player, now: Long, notifyClear: Boolean, notifyRotate: Boolean) {
        if (clearOnRotate()) {
            removeIncompleteTasks(player, notifyClear)
        }
        rotateTask(player)
        PlayerRotateData.set(player.name, now)
        if (notifyRotate) {
            player.sendLang("rotate-auto-rotated")
        }
    }

    // å¯é€‰ï¼šå…¨æœå®šæ—¶æ£€æŸ¥åˆ°æœŸè‡ªåŠ¨è½®æ?
    fun startAutoRotateTask() {
        autoTask?.cancel()
        if (!autoRotateEnabled()) return
        val intervalSeconds = CobbleHunt.config.getLong("rotateOptions.auto.checkIntervalSeconds", 60L)
            .coerceAtLeast(5L)
        autoTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            BukkitPlugin.getInstance(),
            kotlinx.coroutines.Runnable {
                if (!rotationEnabled()) return@Runnable
                Bukkit.getOnlinePlayers().forEach { player ->
                    val now = System.currentTimeMillis()
                    val last = PlayerRotateData.get(player.name)
                    val intervalMillis = rotateMinutes.coerceAtLeast(1) * 60 * 1000L
                    val expired = now - last >= intervalMillis
                    val firstTime = last == 0L && autoRotateIfNoRecord()
                    if (expired || firstTime) {
                        val notify = autoRotateNotify()
                        rotateNow(player, now, notifyClear = notify, notifyRotate = notify)
                    }
                }
            },
            intervalSeconds * 20L,
            intervalSeconds * 20L
        )
    }
}
