package io.github.yuazer.cobblehunt.gui

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.api.TaskApi
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.impl.ChestImpl
import taboolib.platform.util.buildItem
import taboolib.platform.util.sendLang

class RotateGui(val player: Player) {
    fun getTitle(): String {
        return (CobbleHunt.rotateGui.getString("title") ?: "&a&l当前轮换任务").colored()
    }

    fun getLayout(): Array<String> {
        return CobbleHunt.rotateGui.getStringList("layout").toTypedArray()
    }

    fun getItems(): Set<String> {
        return CobbleHunt.rotateGui.getConfigurationSection("Items")?.getKeys(false) ?: mutableSetOf()
    }

    private fun getRotateByStarSlots(): Map<Int, List<Int>>? {
        val section = CobbleHunt.rotateGui.getConfigurationSection("rotateByStar") ?: return null
        val result = mutableMapOf<Int, List<Int>>()
        section.getKeys(false).forEach { starKey ->
            val star = starKey.toIntOrNull() ?: return@forEach
            val slots = CobbleHunt.rotateGui.getStringList("rotateByStar.$starKey.slots")
                .mapNotNull { it.toIntOrNull() }
            if (slots.isNotEmpty()) {
                result[star] = slots
            }
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun resolveSlotInfo(slot: Int): Pair<Int, Int>? {
        val byStar = getRotateByStarSlots()
        if (byStar != null) {
            byStar.forEach { (star, slots) ->
                val index = slots.indexOf(slot)
                if (index >= 0) {
                    return star to index
                }
            }
        }
        val star = CobbleHunt.rotateGui.getInt("rotate.$slot.star", -1)
        if (star <= 0) return null
        val index = CobbleHunt.rotateGui.getInt("rotate.$slot.index", 0)
        return star to index
    }

    private fun getRotateSlots(): Set<Int> {
        val byStar = getRotateByStarSlots()
        if (byStar != null) {
            return byStar.values.flatten().toSet()
        }
        return CobbleHunt.rotateGui.getConfigurationSection("rotate")?.getKeys(false)
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
    }

    private fun getOrderedTaskNamesByStar(star: Int): List<String> {
        val order = CobbleHunt.rotateGui.getString("taskOrder", "NAME")?.uppercase() ?: "NAME"
        val tasks = TaskApi.getPlayerTaskNamesByStar(player.name, star)
        return when (order) {
            "NONE" -> tasks
            else -> tasks.sorted()
        }
    }

    fun openMenu() {
        player.openMenu<ChestImpl>(getTitle()) {
            map(
                *getLayout()
            )
            getItems().forEach {
                set(
                    it.toCharArray()[0],
                    buildItem(XMaterial.matchXMaterial(Material.getMaterial(CobbleHunt.rotateGui.getString("Items.${it}.material")!!)!!)) {
                        name = CobbleHunt.rotateGui.getString("Items.${it}.name")
                        CobbleHunt.rotateGui.getStringList("Items.${it}.lore").forEach { l ->
                            lore.add(l)
                        }
                        colored()
                    })
            }
            //éåŽ†è½®æ¢ä»»åŠ¡,å¸ƒç½®ç•Œé¢
            getRotateSlots().forEach { slot ->
                val slotInfo = resolveSlotInfo(slot) ?: return@forEach
                val star = slotInfo.first
                val index = slotInfo.second
                val taskNames = getOrderedTaskNamesByStar(star)
                val taskName = if (index < taskNames.size) taskNames[index] else null

                if (taskName == null) {
                    // æ— ä»»åŠ¡æ—¶ç”?default
                    val itemIcon = buildItem(
                        XMaterial.matchXMaterial(
                            Material.getMaterial(
                                CobbleHunt.icons.getString("default.material") ?: "STONE"
                            ) ?: Material.STONE
                        )
                    ) {
                        name = CobbleHunt.icons.getString("default.name")
                        lore.addAll(CobbleHunt.icons.getStringList("default.lore"))
                        colored()
                    }
                    set(slot, itemIcon)
                    return@forEach
                }

                val progressMap = TaskApi.getPlayerTaskProgressMap(player.name)[taskName]
                val iconPath = "tasks.${taskName}"
                val itemIcon = buildItem(
                    XMaterial.matchXMaterial(
                        Material.getMaterial(
                            CobbleHunt.icons.getString("${iconPath}.material") ?: "STONE"
                        ) ?: Material.STONE
                    )
                ) {
                    name = CobbleHunt.icons.getString("${iconPath}.name")
                    val rawLore = CobbleHunt.icons.getStringList("${iconPath}.lore")
                    val replacedLore = rawLore.map { line ->
                        line.replace("%state%", TaskApi.getTaskStatus(player.name, taskName).inChinese())
                            .replace(Regex("%(.*?)%")) { matchResult ->
                                val key = matchResult.groupValues[1]
                                (progressMap?.get(key)?.toString() ?: "0")
                            }
                    }
                    lore.addAll(replacedLore)
                    colored()
                }
                set(slot, itemIcon)
            }
            onClick(true) { event ->
                val slot = event.rawSlot
                val slotInfo = resolveSlotInfo(slot) ?: return@onClick
                val star = slotInfo.first
                val index = slotInfo.second
                val taskNames = getOrderedTaskNamesByStar(star)
                val taskName = if (index < taskNames.size) taskNames[index] else null
                if (taskName == null) {
                    return@onClick
                }
                //æäº¤ä»»åŠ¡
                if (!TaskApi.submitTask(player.name, taskName)) {
                    player.sendLang("task-not-completed")
                } else {
                    player.sendLang("task-completed")
                }
                player.closeInventory()
            }
        }
    }
}
