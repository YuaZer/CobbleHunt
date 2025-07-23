package io.github.yuazer.cobblehunt.data

import io.github.yuazer.cobblehunt.enums.TaskStatus
import io.github.yuazer.cobblehunt.model.HuntTask
import io.github.yuazer.cobblehunt.model.map.DoubleKeyMap
import io.github.yuazer.cobblehunt.model.map.StringListMap
import io.github.yuazer.cobblehunt.model.map.TripleKeyMap
import taboolib.common.io.newFolder
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type

object DataLoader {
    // 任务名,任务对象 map
    val taskMap = mutableMapOf<String,HuntTask>()
    // 玩家名,任务名 map
    val taskCountMap = TripleKeyMap<String,String,String,Int>()
    // 玩家名,任务名 map(玩家正在进行的任务)
    val playerTaskingMap = StringListMap()

    // 玩家名,任务名,任务状态 map
    val playerTaskStatusMap = DoubleKeyMap<String, String, TaskStatus>()
    fun reload(){
        taskMap.clear()
        val file = newFolder(getDataFolder(), "tasks", create = true)
        // 文件不存在则释放jar内的文件
        if (!file.exists()) {
            file.mkdirs()
            releaseResourceFile("tasks/example.yml")
        }
        file.walk()
            .filter { it.isFile }
            .filter { it.extension == "yaml" || it.extension == "yml" }
            .forEach {
                val yamlConfig = Configuration.loadFromFile(it, Type.YAML)
                yamlConfig.getKeys(false).forEach { name->
                    println("§a加载狩猎任务配置:§b$name")
                    val taskSection = yamlConfig.getConfigurationSection(name)
                    taskMap[name] = HuntTask(taskSection!!)
                }
            }
    }
}