package io.github.yuazer.cobblehunt.model.map

/**
 * 类似于 Map<String, List<String>> 的数据结构
 * 提供常用增删改查，并对空值做安全处理
 */
class StringListMap {

    // 内部存储用 MutableList
    private val map = HashMap<String, MutableList<String>>()

    /** 查询：取key对应的List，如果无则返回空List（只读，防止外部直接修改） */
    operator fun get(key: String): List<String> = map[key] ?: emptyList()

    /** 判断是否有该key */
    fun containsKey(key: String): Boolean = map.containsKey(key)

    /** 插入或追加一个元素到某key下 */
    fun add(key: String, value: String) {
        map.getOrPut(key) { mutableListOf() }.add(value)
    }

    /** 插入多个元素到某key下 */
    fun addAll(key: String, values: Collection<String>) {
        map.getOrPut(key) { mutableListOf() }.addAll(values)
    }

    /** 替换某key的List为新的List */
    fun set(key: String, values: Collection<String>) {
        map[key] = values.toMutableList()
    }

    /** 删除key整个List */
    fun remove(key: String): List<String>? = map.remove(key)

    /** 删除某key下的某个元素 */
    fun removeValue(key: String, value: String): Boolean =
        map[key]?.remove(value) ?: false

    /** 清空所有数据 */
    fun clear() = map.clear()

    /** 所有key集合 */
    fun keys(): Set<String> = map.keys

    /** 所有value集合（所有List，注意：是原始的MutableList，可以toList()防护） */
    fun values(): Collection<List<String>> = map.values.map { it.toList() }

    /** 大小（key数量） */
    val size: Int get() = map.size

    /** 是否为空 */
    fun isEmpty() = map.isEmpty()

    /** 遍历所有 (key, List) */
    fun forEach(action: (String, List<String>) -> Unit) {
        map.forEach { (k, v) -> action(k, v.toList()) }
    }

    override fun toString(): String = map.toString()
}
