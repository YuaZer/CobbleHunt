package io.github.yuazer.cobblehunt.listen

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.events.pokemon.LevelUpEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionAcceptedEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent
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
        CobblemonEvents.LEVEL_UP_EVENT.subscribe { event ->
            onLevelUp(event)
        }
        CobblemonEvents.EVOLUTION_COMPLETE.subscribe { event ->
            onEvolvePost(event)
        }
        CobblemonEvents.EVOLUTION_ACCEPTED.subscribe { event ->
            onEvolvePre(event)
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
                if (countCondition.type != TaskApi.CAPTURE_PROGRESS_PREFIX_KEY) continue
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
                ?: //                println("玩家不存在")
                continue@playersLoop
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
                    if (countCondition.type != TaskApi.BEAT_PROGRESS_PREFIX_KEY) continue
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

    fun onLevelUp(event: LevelUpEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        val tasks = TaskApi.getPlayerTasks(player.name)
        for (taskName in tasks) {
            val task = TaskApi.getTask(taskName) ?: continue
            // 遍历所有 countConditions，type 必须是 "capture"
            for ((progressKey, countCondition) in task.countConditions) {
                if (countCondition.type != TaskApi.LEVEL_UP_PROGRESS_PREFIX_KEY) continue
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

    fun onEvolvePost(event: EvolutionCompleteEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        val tasks = TaskApi.getPlayerTasks(player.name)
        for (taskName in tasks) {
            val task = TaskApi.getTask(taskName) ?: continue
            // 遍历所有 countConditions，type 必须是 "capture"
            for ((progressKey, countCondition) in task.countConditions) {
                if (countCondition.type != TaskApi.EVOLVE_POST_PROGRESS_PREFIX_KEY) continue
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

    fun onEvolvePre(event: EvolutionAcceptedEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        val tasks = TaskApi.getPlayerTasks(player.name)
        for (taskName in tasks) {
            val task = TaskApi.getTask(taskName) ?: continue
            // 遍历所有 countConditions，type 必须是 "capture"
            for ((progressKey, countCondition) in task.countConditions) {
                if (countCondition.type != TaskApi.EVOLVE_PRE_PROGRESS_PREFIX_KEY) continue
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
//    fun onBattleInstruction(event: BattleInstructionEvent) {
//        event.battle.players.forEach { serverPlayer ->
//            val player = Bukkit.getPlayer(serverPlayer.uuid) ?: return
//            val tasks = TaskApi.getPlayerTasks(player.name)
//            val pokemon = event.battle.activePokemon.first {
//                val uuid = it.battlePokemon?.originalPokemon?.getOwnerUUID()
//                if (uuid == null) return@forEach
//                else uuid.toString() == player.uniqueId.toString()
//            }.battlePokemon?.originalPokemon ?: return
//            for (taskName in tasks) {
//                val task = TaskApi.getTask(taskName) ?: continue
//                for ((progressKey, countCondition) in task.countConditions) {
//                    if (countCondition.type != TaskApi.BATTLE_INSTRUCTION_SELF_PREFIX_KEY) continue
//                    if (countCondition.conditions.isEmpty() ||
//                        ScriptUtils.evalListToBoolean(
//                            countCondition.conditions,
//                            pokemon
//                        )
//                    ) {
//                        TaskApi.addTaskProgress(player.name, taskName, progressKey)
//                    }
//                }
//            }
//        }
//    }
}
