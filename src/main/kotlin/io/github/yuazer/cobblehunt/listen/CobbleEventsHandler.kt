package io.github.yuazer.cobblehunt.listen

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import io.github.yuazer.cobblehunt.api.TaskApi
import io.github.yuazer.cobblehunt.utils.ScriptUtils
import org.bukkit.Bukkit

object CobbleEventsHandler {
    fun register() {
        CobblemonEvents.BATTLE_FAINTED.subscribe { event ->
            onBeat(event)
        }
        CobblemonEvents.POKEMON_CAPTURED.subscribe { event ->
            onCapture(event)
        }
    }

    fun onCapture(event: PokemonCapturedEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(event.player.uuid) ?: return
        val tasks = TaskApi.getPlayerTasks(player.name)
        for (taskName in tasks) {
            val task = TaskApi.getTask(taskName) ?: continue
            // 遍历所有 countConditions，type 必须是 "capture"
            for ((progressKey, countCondition) in task.countConditions) {
                if (countCondition.type != "capture") continue
                if (countCondition.conditions.isEmpty() ||
                    ScriptUtils.evalListToBoolean(
                        countCondition.conditions,
                        pokemon
                    )
                ) {
                    TaskApi.addTaskProgress(player.name, taskName, progressKey)
                }
            }
        }
    }

    fun onBeat(event: BattleFaintedEvent) {
        if (!event.battle.isPvW) return
        playersLoop@ for (serverPlayer in event.battle.players) {
            val player = Bukkit.getPlayer(serverPlayer.uuid)
            if (player == null) {
//                println("玩家不存在")
                continue@playersLoop
            }
            val pokemon = event.killed.originalPokemon
            val pokeOwnerUUID = pokemon.getOwnerUUID()
            // 确认击杀宝可梦不属于该玩家
            if (pokeOwnerUUID != null && (pokeOwnerUUID.toString() == player.uniqueId.toString())) {
//                println("玩家精灵死亡,跳过计算")
                continue
            }
            val tasks = TaskApi.getPlayerTasks(player.name)
            for (taskName in tasks) {
                val task = TaskApi.getTask(taskName) ?: continue
                // 遍历所有 countConditions，type 必须是 "beat"
                for ((progressKey, countCondition) in task.countConditions) {
                    if (countCondition.type != "beat") continue
                    if (countCondition.conditions.isEmpty() ||
                        ScriptUtils.evalListToBoolean(
                            countCondition.conditions,
                            pokemon
                        )
                    ) {
                        TaskApi.addTaskProgress(player.name, taskName, progressKey)
                    }
                }
            }
        }
    }
}
