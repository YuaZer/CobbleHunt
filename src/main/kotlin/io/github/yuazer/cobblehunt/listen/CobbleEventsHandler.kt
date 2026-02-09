package io.github.yuazer.cobblehunt.listen

import com.cobblemon.mod.common.api.events.CobblemonEvents
import com.cobblemon.mod.common.api.events.battles.BattleFaintedEvent
import com.cobblemon.mod.common.api.events.pokemon.LevelUpEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonFaintedEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonRecallEvent
import com.cobblemon.mod.common.api.events.pokemon.PokemonSentEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionAcceptedEvent
import com.cobblemon.mod.common.api.events.pokemon.evolution.EvolutionCompleteEvent
import com.cobblemon.mod.common.api.events.pokemon.healing.PokemonHealedEvent
import com.cobblemon.mod.common.api.events.storage.ReleasePokemonEvent
import com.cobblemon.mod.common.pokemon.Pokemon
import io.github.yuazer.cobblehunt.api.TaskApi
import io.github.yuazer.cobblehunt.utils.ScriptUtils
import org.bukkit.Bukkit
import org.bukkit.entity.Player

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
        CobblemonEvents.POKEMON_FAINTED.subscribe { event ->
            onFaint(event)
        }
        CobblemonEvents.POKEMON_HEALED.subscribe { event ->
            onHeal(event)
        }
        CobblemonEvents.POKEMON_SENT_POST.subscribe { event ->
            onSent(event)
        }
        CobblemonEvents.POKEMON_RECALL_POST.subscribe { event ->
            onRecall(event)
        }
        CobblemonEvents.POKEMON_RELEASED_EVENT_POST.subscribe { event ->
            onRelease(event)
        }
    }

    private fun handlePokemonProgress(player: Player, pokemon: Pokemon, type: String) {
        val tasks = TaskApi.getPlayerTasks(player.name)
        for (taskName in tasks) {
            val task = TaskApi.getTask(taskName) ?: continue
            for ((progressKey, countCondition) in task.countConditions) {
                if (countCondition.type != type) continue
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

    fun onCapture(event: PokemonCapturedEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(event.player.uuid) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.CAPTURE_PROGRESS_PREFIX_KEY)
    }

    fun onBeat(event: BattleFaintedEvent) {
        if (!event.battle.isPvW) return

        playersLoop@ for (serverPlayer in event.battle.players) {
            val player = Bukkit.getPlayer(serverPlayer.uuid)
                ?: run {
                    // 玩家不存在
                    continue@playersLoop
                }

            val pokemon = event.killed.originalPokemon
            val pokeOwnerUUID = pokemon.getOwnerUUID()

            // 确认击杀宝可梦不属于该玩家
            if (pokeOwnerUUID != null && (pokeOwnerUUID.toString() == player.uniqueId.toString())) {
                // 玩家精灵死亡，跳过计算
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
        handlePokemonProgress(player, pokemon, TaskApi.LEVEL_UP_PROGRESS_PREFIX_KEY)
    }

    fun onEvolvePost(event: EvolutionCompleteEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.EVOLVE_POST_PROGRESS_PREFIX_KEY)
    }

    fun onEvolvePre(event: EvolutionAcceptedEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.EVOLVE_PRE_PROGRESS_PREFIX_KEY)
    }

    fun onFaint(event: PokemonFaintedEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.FAINT_PROGRESS_PREFIX_KEY)
    }

    fun onHeal(event: PokemonHealedEvent) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.HEAL_PROGRESS_PREFIX_KEY)
    }

    fun onSent(event: PokemonSentEvent.Post) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.SENT_PROGRESS_PREFIX_KEY)
    }

    fun onRecall(event: PokemonRecallEvent.Post) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(pokemon.getOwnerUUID() ?: return) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.RECALL_PROGRESS_PREFIX_KEY)
    }

    fun onRelease(event: ReleasePokemonEvent.Post) {
        val pokemon = event.pokemon
        val player = Bukkit.getPlayer(event.player.uuid) ?: return
        handlePokemonProgress(player, pokemon, TaskApi.RELEASE_PROGRESS_PREFIX_KEY)
    }
}
