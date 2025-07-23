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
            //遍历轮换任务,布置界面
            val rotateTaskKeys = CobbleHunt.rotateGui.getConfigurationSection("rotate")?.getKeys(false) ?: return
            rotateTaskKeys.forEach { slot ->
                val star = CobbleHunt.rotateGui.getInt("rotate.${slot}.star")
                val index = CobbleHunt.rotateGui.getInt("rotate.${slot}.index")
                val taskNames = TaskApi.getPlayerTaskNamesByStar(player.name, star)
                val taskName = if (index < taskNames.size) taskNames[index] else null

                if (taskName == null) {
                    // 无任务时用 default
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
                    set(slot.toInt(), itemIcon)
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
                set(slot.toInt(), itemIcon)
            }
            onClick(true) { event ->
                val slot = event.rawSlot
                val star = CobbleHunt.rotateGui.getInt("rotate.${slot}.star")
                val index = CobbleHunt.rotateGui.getInt("rotate.${slot}.index")
                val taskNames = TaskApi.getPlayerTaskNamesByStar(player.name, star)
                val taskName = if (index < taskNames.size) taskNames[index] else null
                if (taskName == null) {
                    return@onClick
                }
                //提交任务
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