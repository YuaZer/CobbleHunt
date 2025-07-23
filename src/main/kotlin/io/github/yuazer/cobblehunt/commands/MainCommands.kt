package io.github.yuazer.cobblehunt.commands

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.api.TaskApi
import io.github.yuazer.cobblehunt.data.DataLoader
import io.github.yuazer.cobblehunt.gui.RotateGui
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.command.*
import taboolib.expansion.createHelper
import taboolib.module.chat.colored
import taboolib.module.lang.Language
import taboolib.platform.util.onlinePlayers
import taboolib.platform.util.sendLang

@CommandHeader("cobblehunt", aliases = ["ch", "hunt"], permissionDefault = PermissionDefault.TRUE)
object MainCommands {
    @CommandBody(permission = "cobblehunt.reload")
    val reload = subCommand {
        execute<CommandSender> { sender, context, argument ->
            CobbleHunt.config.reload()
            CobbleHunt.rotateGui.reload()
            CobbleHunt.icons.reload()
            DataLoader.reload()
            CobbleHunt.playerRotateManager.rotateMinutes = CobbleHunt.config.getInt("rotateOptions.time", 480)
            //重载语言文件
            Language.reload()
            sender.sendLang("reload-message")
        }
    }

    @CommandBody
    val help = subCommand {
        createHelper(true)
    }

    @CommandBody(aliases = ["addTask"], permission = "cobblehunt.addTask")
    val add = subCommand {
        player("player") {
            suggestion<CommandSender> { _, _ ->
                onlinePlayers.map { it.name }
            }
            dynamic("taskName") {
                suggestion<CommandSender> { _, _ ->
                    DataLoader.taskMap.keys.toList()
                }
                execute<CommandSender> { sender, context, argument ->
                    val taskName = context["taskName"]
                    if (TaskApi.addTask(context["player"], taskName)) {
                        sender.sendLang("add-task-message")
                    } else {
                        sender.sendLang("task-already-exists")
                    }
                }
            }
        }
    }

    @CommandBody(aliases = ["removeTask"], permission = "cobblehunt.removeTask")
    val remove = subCommand {
        player("player") {
            suggestion<CommandSender> { _, _ ->
                onlinePlayers.map { it.name }
            }
            dynamic("taskName") {
                suggestion<CommandSender>(uncheck = false) { sender, context ->
                    TaskApi.getPlayerTasks(context["player"])
                }
                execute<CommandSender> { sender, context, argument ->
                    val taskName = context["taskName"]
                    if (TaskApi.removeTask(context["player"], taskName)) {
                        sender.sendLang("remove-task-message")
                    } else {
                        sender.sendLang("task-not-exists")
                    }
                }
            }
        }
    }

    @CommandBody(aliases = ["listTasks"], permission = "cobblehunt.listTasks")
    val list = subCommand {
        player("player") {
            suggestion<CommandSender> { _, _ ->
                onlinePlayers.map { it.name }
            }
            execute<CommandSender> { sender, context, argument ->
                val player = context.player("player")
                val tasks = TaskApi.getPlayerTasks(player.name)
                if (tasks.isEmpty()) {
                    sender.sendLang("no-tasks")
                    return@execute
                }
                sender.sendMessage("&a${player.name} &7的&a任务列表:".colored())
                sender.sendMessage("&a${tasks.joinToString(", ")}".colored())
            }
        }
    }

    @CommandBody(permission = "cobblehunt.debug")
    val debug = subCommand {
        execute<CommandSender> { sender, context, argument ->
            if (sender !is Player) {
                sender.sendLang("player-only")
                return@execute
            }
            sender.sendMessage("&a当前任务列表:".colored())
            DataLoader.taskMap.forEach { (name, task) ->
                sender.sendMessage("&a任务名称: &7$name".colored())
                // 展示所有 countConditions
                if (task.countConditions.isEmpty()) {
                    sender.sendMessage("&c暂无计数条件".colored())
                } else {
                    sender.sendMessage("&a计数条件:".colored())
                    task.countConditions.forEach { (key, cond) ->
                        sender.sendMessage(
                            "&7 - &e$key &8| 类型: &b${cond.type} &8| 条件: &f${
                                cond.conditions.joinToString(
                                    " && "
                                )
                            }".colored()
                        )
                    }
                }
                // 展示提交条件
                sender.sendMessage("&a提交条件: &7${task.submitConditions.joinToString(" & ")}".colored())
                // 展示奖励
                sender.sendMessage("&a任务奖励: &7${task.rewards.joinToString(", ")}".colored())
                // 展示进度
                val taskProgressMap = TaskApi.getPlayerTaskAllProgress(sender.name, name)
                if (taskProgressMap.isEmpty()) {
                    sender.sendMessage("&c无进度数据".colored())
                } else {
                    sender.sendMessage("&a任务进度:".colored())
                    taskProgressMap.forEach { (progressKey, progress) ->
                        sender.sendMessage("&7 - &e$progressKey: &b$progress".colored())
                    }
                }
                sender.sendMessage(" ")
            }
        }
    }

    @CommandBody(permission = "cobblehunt.submit")
    val submit = subCommand {
        dynamic("taskName") {
            suggestion<CommandSender> { sender, _ ->
                TaskApi.getPlayerTasks(sender.name)
            }
            execute<CommandSender> { sender, context, argument ->
                val taskName = context["taskName"]
                if (!TaskApi.submitTask(sender.name, taskName)) {
                    sender.sendLang("task-not-completed")
                } else {
                    sender.sendLang("task-completed")
                }
            }
        }
    }

    @CommandBody(aliases = ["rotate"], permission = "cobblehunt.rotate")
    val rotate = subCommand {
        execute<CommandSender> { sender, context, argument ->
            if (sender !is Player) {
                sender.sendLang("player-only")
                return@execute
            }
            val rotateGui = RotateGui(sender)
            rotateGui.openMenu()
        }
    }

    @CommandBody(aliases = ["trotate"], permission = "cobblehunt.tryRotate")
    val tryRotate = subCommand {
        execute<CommandSender> { sender, context, argument ->
            if (sender !is Player) {
                sender.sendLang("player-only")
                return@execute
            }
            CobbleHunt.playerRotateManager.tryRotate(sender)
        }
    }

    @CommandBody(aliases = ["rotateForce"], permission = "cobblehunt.rotateForce")
    val rotateForce = subCommand {
        player("player") {
            suggestion<CommandSender>(uncheck = false) { _, _ ->
                onlinePlayers.map { it.name }
            }
            execute<CommandSender> { sender, context, argument ->
                val player = context.player("player")
                val bukkitPlayer = player.castSafely<Player>()?:return@execute
                CobbleHunt.playerRotateManager.forceRotate(bukkitPlayer)
            }
        }
        execute<CommandSender> { sender, context, argument ->
            if (sender !is Player) {
                sender.sendLang("player-only")
                return@execute
            }
            CobbleHunt.playerRotateManager.forceRotate(sender)
        }
    }
}