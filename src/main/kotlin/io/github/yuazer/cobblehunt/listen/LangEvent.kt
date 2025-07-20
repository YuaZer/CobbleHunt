package io.github.yuazer.cobblehunt.listen

import io.github.yuazer.cobblehunt.CobbleHunt
import taboolib.common.platform.event.SubscribeEvent
import taboolib.module.lang.event.PlayerSelectLocaleEvent
import taboolib.module.lang.event.SystemSelectLocaleEvent

object LangEvent {
    @SubscribeEvent
    fun lang(event: PlayerSelectLocaleEvent) {
        event.locale = CobbleHunt.config.getString("Lang", "zh_CN")!!
    }

    @SubscribeEvent
    fun lang(event: SystemSelectLocaleEvent) {
        event.locale = CobbleHunt.config.getString("Lang", "zh_CN")!!
    }
}