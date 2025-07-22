package io.github.yuazer.cobblehunt.model

import io.github.yuazer.cobblehunt.utils.PlayerUtils.runKether
import org.bukkit.entity.Player
import taboolib.library.configuration.ConfigurationSection

class HuntTask(val configurationSection: ConfigurationSection) {
    val name: String = configurationSection.name
    val star:Int = configurationSection.getInt("star",1)
    /** 奖励支持单行和多行配置 */
    val rewards: List<String> = configurationSection.getStringList("rewards").let {
        it.ifEmpty {
            configurationSection.getString("rewards")?.let { s -> listOf(s) } ?: emptyList()
        }
    }

    /** 提交条件 */
    val submitConditions: List<String> = configurationSection.getStringList("submitConditions")

    /** 计数条件数据类 */
    data class CountCondition(
        val type: String,
        val conditions: List<String>
    )

    /** Map<条件名, CountCondition> */
    val countConditions: Map<String, CountCondition> =
        configurationSection.getConfigurationSection("countConditions")?.let { sec ->
            sec.getKeys(false).associateWith { key ->
                val sub = sec.getConfigurationSection(key)!!
                CountCondition(
                    type = sub.getString("type") ?: "",
                    conditions = sub.getStringList("conditions")
                )
            }
        } ?: emptyMap()

    fun runRewards(player: Player) {
        rewards.runKether(player)
    }

    override fun toString(): String {
        return "HuntTask(name='$name', rewards=$rewards, countConditions=$countConditions, submitConditions=$submitConditions)"
    }
}
