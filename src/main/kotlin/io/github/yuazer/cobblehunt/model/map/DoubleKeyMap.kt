package io.github.yuazer.cobblehunt.model.map

/**
 * 支持通过两个 key 获取 value 的泛型 Map
 * 用法示例：val map = DoubleKeyMap<String, String, Int>()
 */
class DoubleKeyMap<A, B, V> : Cloneable {

    private val backingMap = HashMap<Pair<A, B>, V>()

    /** 获取值 */
    operator fun get(a: A, b: B): V? = backingMap[Pair(a, b)]

    /** 设置值 */
    operator fun set(a: A, b: B, value: V) {
        backingMap[Pair(a, b)] = value
    }

    /** 添加/修改 */
    fun put(a: A, b: B, value: V): V? = backingMap.put(Pair(a, b), value)

    /** 批量添加/修改 */
    fun putAll(other: DoubleKeyMap<A, B, V>) {
        backingMap.putAll(other.backingMap)
    }

    /** 查询是否存在指定键 */
    fun containsKeys(a: A, b: B): Boolean = backingMap.containsKey(Pair(a, b))

    /** 查询是否存在值 */
    fun containsValue(value: V): Boolean = backingMap.containsValue(value)

    /** 删除指定键对应的值 */
    fun remove(a: A, b: B): V? = backingMap.remove(Pair(a, b))

    /** 清空全部内容 */
    fun clear() = backingMap.clear()

    /** 键值对数量 */
    val size: Int get() = backingMap.size

    /** 是否为空 */
    fun isEmpty() = backingMap.isEmpty()

    /** 获取所有 Pair 形式的 key */
    fun keys(): Set<Pair<A, B>> = backingMap.keys

    /** 获取所有 key1 的去重集合 */
    fun keys1(): Set<A> = backingMap.keys.map { it.first }.toSet()

    /** 获取所有 key2 的去重集合 */
    fun keys2(): Set<B> = backingMap.keys.map { it.second }.toSet()

    /** 获取所有 value */
    fun values(): Collection<V> = backingMap.values

    /** 获取所有 Map.Entry<Pair<A,B>,V> */
    fun entries(): Set<Map.Entry<Pair<A, B>, V>> = backingMap.entries

    /** 查找所有 key1=指定值 的 (key2,value) 集合 */
    fun getByFirstKey(a: A): List<Pair<B, V>> =
        backingMap.filterKeys { it.first == a }.map { it.key.second to it.value }

    /** 查找所有 key2=指定值 的 (key1,value) 集合 */
    fun getBySecondKey(b: B): List<Pair<A, V>> =
        backingMap.filterKeys { it.second == b }.map { it.key.first to it.value }

    /** 克隆一个新实例（深拷贝，value 仍为引用，适合基本数据类型或不可变对象） */
    public override fun clone(): DoubleKeyMap<A, B, V> {
        val copy = DoubleKeyMap<A, B, V>()
        copy.backingMap.putAll(this.backingMap)
        return copy
    }

    /** 遍历所有键值对 */
    fun forEach(action: (A, B, V) -> Unit) {
        backingMap.forEach { (pair, v) -> action(pair.first, pair.second, v) }
    }
    fun getOrDefault(a: A, b: B, default: V): V = backingMap[Pair(a, b)] ?: default

    /** 支持解构，返回 (Pair<A,B>, V) 的序列 */
    operator fun iterator(): Iterator<Map.Entry<Pair<A, B>, V>> = backingMap.entries.iterator()

    override fun toString(): String = backingMap.toString()
}
