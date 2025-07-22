package io.github.yuazer.cobblehunt.gui

import io.github.yuazer.cobblehunt.CobbleHunt
import io.github.yuazer.cobblehunt.api.TaskApi
import org.bukkit.Material
import org.bukkit.entity.Player
import taboolib.common5.util.replace
import taboolib.library.xseries.XMaterial
import taboolib.module.chat.colored
import taboolib.module.ui.openMenu
import taboolib.module.ui.type.impl.ChestImpl
import taboolib.platform.util.buildItem

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
                val taskName = TaskApi.getPlayerTaskNamesByStar(player.name, star)[index]
                val progressMap = TaskApi.getPlayerTaskProgressMap(player.name)[taskName]
                val itemIcon = buildItem(
                    XMaterial.matchXMaterial(
                        Material.getMaterial(
                            CobbleHunt.icons.getString("${taskName}.material") ?: "STONE"
                        ) ?: Material.STONE
                    )
                ) {
                    name = CobbleHunt.icons.getString("${taskName}.name")
                    //给lore中的进度变量全部替换
                    val rawLore = CobbleHunt.icons.getStringList("${taskName}.lore")
                    val replacedLore = rawLore.map { line ->
                        // 用正则替换所有 %xxx%
                        line.replace(Regex("%(.*?)%")) { matchResult ->
                            val key = matchResult.groupValues[1]
                            (progressMap?.get(key)?.toString() ?: "0")
                        }.replace("%state%", TaskApi.getTaskStatus(player.name, taskName).inChinese())
                    }
                    lore.addAll(replacedLore)
                    colored()
                }
                set(slot.toInt(), itemIcon)
            }
            onClick(true) {

            }
        }
    }
}