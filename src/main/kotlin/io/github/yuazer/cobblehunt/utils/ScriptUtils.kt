package io.github.yuazer.cobblehunt.utils

import com.cobblemon.mod.common.api.pokemon.stats.Stats
import com.cobblemon.mod.common.pokemon.Pokemon
import com.cobblemon.mod.common.pokemon.activestate.ShoulderedState
import com.cobblemon.mod.common.util.DataKeys
import com.cobblemon.mod.common.util.isPokemonEntity
import com.cobblemon.mod.common.util.party
import io.github.yuazer.cobblehunt.api.TaskApi
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.platform.compat.replacePlaceholder
import taboolib.platform.util.asLangText
import top.maplex.arim.Arim
import kotlin.collections.all
import kotlin.collections.elementAtOrNull
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.collections.mapValues
import kotlin.let
import kotlin.math.roundToInt
import kotlin.text.replace
import kotlin.text.toDoubleOrNull

object ScriptUtils {
    fun evalToInt(expression: String, variables: Map<String, Any> = emptyMap()): Int {
        // 将变量替换到表达式中
        var parsedExpression = expression
        variables.forEach { (key, value) ->
            parsedExpression = parsedExpression.replace(key, value.toString(), ignoreCase = true)
        }
        // 使用 Arim 解析表达式
        return when (val result = Arim.fixedCalculator.evaluate(parsedExpression)) {
            else -> result.roundToInt()
        }
    }

    fun evalToDouble(expression: String, variables: Map<String, Any> = emptyMap()): Double {
        // 将变量替换到表达式中
        var parsedExpression = expression
        variables.forEach { (key, value) ->
            parsedExpression = parsedExpression.replace(key, value.toString(), ignoreCase = true)
        }
        // 使用 Arim 解析表达式
        return Arim.fixedCalculator.evaluate(parsedExpression)
    }


    fun evalToInt(expression: String, pokemon: Pokemon): Int {
        return evalToInt(expression, pokemonPapiToMap(pokemon))
    }

    fun evalToDouble(expression: String, pokemon: Pokemon): Double {
        return evalToDouble(expression, pokemonPapiToMap(pokemon))
    }

    fun evalToBoolean(expression: String, variables: Map<String, Any> = emptyMap()): Boolean {
        var parsedExpression = expression
        variables.forEach { (key, value) ->
            parsedExpression = parsedExpression.replace(key, value.toString(), ignoreCase = true)
        }

        val arimVars = variables.mapValues { (_, value) ->
            when (value) {
                is String -> value.toDoubleOrNull() ?: value
                else -> value
            }
        }
        return try {
            val result = Arim.evaluator.evaluate(parsedExpression, arimVars)
            if (!result) {
                println("当前条表达式计算结果为false: \"$expression\"\n替换后: \"$parsedExpression\"")
            }
            result
        } catch (e: Exception) {
            println("布尔表达式计算失败: \"$expression\"\n替换后: \"$parsedExpression\"\n变量: $arimVars")
            false
        }
    }


    fun evalToBoolean(expression: String, pokemon: Pokemon): Boolean {
        return evalToBoolean(expression, pokemonPapiToMap(pokemon))
    }

    /**
     * 自动将任务进度带入表达式进行逻辑判断
     * @param expression 形如 "%capture_Absol% >= 3 && %defeat_Absol% >= 2"
     * @param player 玩家名
     * @param taskName 任务名
     * @return Boolean
     */
    fun evalTaskExpression(expression: String, player: String, taskName: String): Boolean {
        // 获取该玩家该任务所有进度项
        val progressMap = TaskApi.getPlayerTaskAllProgress(player, taskName)
        // 变量Map："%xxx%" -> 进度值
        val variableMap = progressMap.mapKeys { "%${it.key}%" }
        return evalToBoolean(expression, variableMap)
    }

    fun evalTaskListExpression(expression: List<String>, player: String, taskName: String): Boolean {
        val canPass = expression.all { evalTaskExpression(it, player, taskName) }
        if (!canPass) {
            Bukkit.getPlayer(player)?.sendMessage(Bukkit.getPlayer(player)?.asLangText("eval-not-pass"))
        }
        return canPass
    }

    fun evalListToBoolean(expression: List<String>, variables: Map<String, Any> = emptyMap()): Boolean {
        if (expression.isEmpty()) return true
        return expression.all { evalToBoolean(it, variables) }
    }

    fun evalListToBoolean(expression: List<String>, variables: Map<String, Any> = emptyMap(), player: Player): Boolean {
        if (expression.isEmpty()) return true
        val canPass = expression.all { evalToBoolean(it.replacePlaceholder(player), variables) }
        if (!canPass) {
            player.sendMessage(player.asLangText("eval-not-pass"))
        }
        return canPass
    }

    fun evalListToBoolean(expression: List<String>, pokemon: Pokemon): Boolean {
        if (expression.isEmpty()) return true
        return expression.all { evalToBoolean(it, pokemonPapiToMap(pokemon)) }
    }

    fun pokemonPapiToMap(pokemon: Pokemon): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["%pokemon_name%"] = pokemon.species.name.replace(" ", "#")
        map["%pokemon_form%"] = pokemon.form.name
        map["%pokemon_shiny%"] = pokemon.shiny
        map["%pokemon_level%"] = pokemon.level
        map["%pokemon_exp%"] = pokemon.experience
        map["%pokemon_exp_to_next_level%"] = pokemon.getExperienceToNextLevel()
        map["%pokemon_exp_percentage_to_next_level%"] =
            (pokemon.experience.toDouble() / pokemon.getExperienceToNextLevel().toDouble()).roundToInt() * 100

        // IVs
        map["%pokemon_ivs_attack%"] = pokemon.ivs[Stats.ATTACK] ?: 0
        map["%pokemon_ivs_defence%"] = pokemon.ivs[Stats.DEFENCE] ?: 0
        map["%pokemon_ivs_hp%"] = pokemon.ivs[Stats.HP] ?: 0
        map["%pokemon_ivs_speed%"] = pokemon.ivs[Stats.SPEED] ?: 0
        map["%pokemon_ivs_special_attack%"] = pokemon.ivs[Stats.SPECIAL_ATTACK] ?: 0
        map["%pokemon_ivs_special_defence%"] = pokemon.ivs[Stats.SPECIAL_DEFENCE] ?: 0
        // IV总和
        var ivsTotal = 0
        pokemon.ivs.forEach { (_, v) ->
            ivsTotal += v
        }
        map["%pokemon_ivs_total%"] = ivsTotal

        // EVs
        map["%pokemon_evs_attack%"] = pokemon.evs[Stats.ATTACK] ?: 0
        map["%pokemon_evs_defence%"] = pokemon.evs[Stats.DEFENCE] ?: 0
        map["%pokemon_evs_hp%"] = pokemon.evs[Stats.HP] ?: 0
        map["%pokemon_evs_speed%"] = pokemon.evs[Stats.SPEED] ?: 0
        map["%pokemon_evs_special_attack%"] = pokemon.evs[Stats.SPECIAL_ATTACK] ?: 0
        map["%pokemon_evs_special_defence%"] = pokemon.evs[Stats.SPECIAL_DEFENCE] ?: 0
        //努力值总和
        var evsTotal = 0
        pokemon.evs.forEach { (_, v) ->
            evsTotal += v
        }
        map["%pokemon_evs_total%"] = evsTotal

        // 属性类型
        map["%pokemon_type_1%"] = pokemon.types.elementAtOrNull(0)?.name ?: "NONE"
        map["%pokemon_type_2%"] = pokemon.types.elementAtOrNull(1)?.name ?: "NONE"
        //是否是肩膀
        map["%pokemon_is_shoulder%"] = pokemon.state is ShoulderedState
        val player = pokemon.getOwnerPlayer()
        player?.let { p ->
            val playerUUID = pokemon.getOwnerUUID()
            playerUUID?.let {
                map["%player_name%"] = Bukkit.getPlayer(it)?.name ?: "NULL/NONE"
            }
            if (p.shoulderEntityLeft.isPokemonEntity()) {
                val shoulder = player.party().first {
                    it.uuid == p.shoulderEntityLeft.getCompound(DataKeys.POKEMON).getUUID(DataKeys.POKEMON_UUID)
                }
                map["%left_shoulder_name%"] = shoulder.species.name.replace(" ", "#")
            }
            if (p.shoulderEntityRight.isPokemonEntity()) {
                val shoulder = player.party().first {
                    it.uuid == p.shoulderEntityRight.getCompound(DataKeys.POKEMON).getUUID(DataKeys.POKEMON_UUID)
                }
                map["%right_shoulder_name%"] = shoulder.species.name.replace(" ", "#")
            }
        }
        return map
    }
}
