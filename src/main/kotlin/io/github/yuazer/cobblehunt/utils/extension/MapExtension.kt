package io.github.yuazer.cobblehunt.utils.extension

import io.github.yuazer.cobblehunt.model.map.StringListMap
import io.github.yuazer.cobblehunt.model.map.TripleKeyMap
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import java.io.File

object MapExtension {
    // 保存为YML（适用于 TripleKeyMap<String, String, String, Int>）
    fun TripleKeyMap<String, String, String, Int>.saveToYaml(file: File) {
        val config = Configuration.empty(Type.YAML)
        this.keys().forEach { (a, b, c) ->
            val path = "$a.$b.$c"
            config[path] = this[a, b, c]
        }
        config.saveToFile(file)
    }
    fun TripleKeyMap<String, String, String, Int>.savePlayerToYaml(player: String, file: File) {
        val config = Configuration.empty(Type.YAML)
        this.keys()
            .filter { it.first == player }
            .forEach { (a, b, c) ->
                val path = "$a.$b.$c"
                config[path] = this[a, b, c]
            }
        config.saveToFile(file)
    }
    // 从YML读取
    fun TripleKeyMap<String, String, String, Int>.loadFromYaml(file: File) {
        val config = Configuration.loadFromFile(file, Type.YAML)
        this.clear()
        for (a in config.getKeys(false)) {
            val secA = config.getConfigurationSection(a) ?: continue
            for (b in secA.getKeys(false)) {
                val secB = secA.getConfigurationSection(b) ?: continue
                for (c in secB.getKeys(false)) {
                    val value = secB.getInt(c)
                    this[a, b, c] = value
                }
            }
        }
    }
    fun TripleKeyMap<String, String, String, Int>.loadPlayerFromYaml(player: String, file: File) {
        val config = Configuration.loadFromFile(file, Type.YAML)
        val secA = config.getConfigurationSection(player) ?: return
        // 可选：清理该玩家的所有历史记录
        this.keys()
            .filter { it.first == player }
            .forEach { (a, b, c) -> this.remove(a, b, c) }

        for (b in secA.getKeys(false)) {
            val secB = secA.getConfigurationSection(b) ?: continue
            for (c in secB.getKeys(false)) {
                val value = secB.getInt(c)
                this[player, b, c] = value
            }
        }
    }
    /**
     * 保存全部数据到 YAML 文件
     */
    fun StringListMap.saveToYaml(file: File) {
        val config = Configuration.loadFromFile(file, Type.YAML)
        this.keys().forEach { key ->
            val valueList = this[key]
            config[key] = valueList // List<String> 可直接写入
        }
        config.saveToFile(file)
    }

    /**
     * 只保存指定 key（如玩家）到 YAML 文件
     */
    fun StringListMap.saveKeyToYaml(key: String, file: File) {
        val config = Configuration.loadFromFile(file, Type.YAML)
        config[key] = this[key]
        config.saveToFile(file)
    }

    /**
     * 从 YAML 文件加载全部数据（会覆盖已有内容）
     */
    fun StringListMap.loadFromYaml(file: File) {
        val config = Configuration.loadFromFile(file, Type.YAML)
        this.clear()
        for (key in config.getKeys(false)) {
            val list = config.getStringList(key)
            this.set(key, list)
        }
    }

    /**
     * 只从 YAML 文件加载指定 key（如玩家）数据（覆盖该 key 原有内容，其它 key 不动）
     */
    fun StringListMap.loadKeyFromYaml(key: String, file: File) {
        val config = Configuration.loadFromFile(file, Type.YAML)
        val list = config.getStringList(key)
        this.set(key, list)
    }
}