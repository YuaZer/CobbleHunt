package io.github.yuazer.cobblehunt.enums

import io.github.yuazer.cobblehunt.CobbleHunt
import taboolib.module.chat.colored

enum class TaskStatus {
    NOT_TAKEN,    // 未接取
    IN_PROGRESS,  // 已接取
    COMPLETED     // 已完成
    ;

    //stateLanguage:
//  task-has: "&a已接取"
//  task-not-has: "&c未接取"
//  task-has-completed: "&a已完成"
    fun inChinese(): String {
        return when (this) {
            NOT_TAKEN -> (CobbleHunt.config.getString("stateLanguage.task-has") ?: "&a已接取").colored()
            IN_PROGRESS -> (CobbleHunt.config.getString("stateLanguage.task-not-has") ?: "&c未接取").colored()
            COMPLETED -> (CobbleHunt.config.getString("stateLanguage.task-has-completed") ?: "&a已完成").colored()
        }
    }
}
