package io.github.yuazer.cobblehunt.model.map

import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import java.io.File

/**
 * 三键Map：支持通过(A,B,C)唯一索引一个值V
 * 例如 TripleKeyMap<String, String, String, Int>()
 * 用法：map["a","b","c"] = 100
 */
class TripleKeyMap<A, B, C, V> : Cloneable {

    private val backingMap = HashMap<Triple<A, B, C>, V>()

    /** 获取值 */
    operator fun get(a: A, b: B, c: C): V? = backingMap[Triple(a, b, c)]

    /** 设置值 */
    operator fun set(a: A, b: B, c: C, value: V) {
        backingMap[Triple(a, b, c)] = value
    }

    /** 添加/修改值（返回旧值或null） */
    fun put(a: A, b: B, c: C, value: V): V? = backingMap.put(Triple(a, b, c), value)

    /** 批量添加/修改（复制另一个TripleKeyMap的数据） */
    fun putAll(other: TripleKeyMap<A, B, C, V>) {
        backingMap.putAll(other.backingMap)
    }

    /** 删除指定键对应的值 */
    fun remove(a: A, b: B, c: C): V? = backingMap.remove(Triple(a, b, c))

    /** 是否包含指定三个key */
    fun containsKeys(a: A, b: B, c: C): Boolean = backingMap.containsKey(Triple(a, b, c))

    /** 是否包含某值 */
    fun containsValue(value: V): Boolean = backingMap.containsValue(value)

    /** 获取所有键的三元组集合 */
    fun keys(): Set<Triple<A, B, C>> = backingMap.keys

    /** 获取所有第一个key的去重集合 */
    fun keys1(): Set<A> = backingMap.keys.map { it.first }.toSet()

    /** 获取所有第二个key的去重集合 */
    fun keys2(): Set<B> = backingMap.keys.map { it.second }.toSet()

    /** 获取所有第三个key的去重集合 */
    fun keys3(): Set<C> = backingMap.keys.map { it.third }.toSet()

    /** 获取所有 value 集合 */
    fun values(): Collection<V> = backingMap.values

    /** 大小 */
    val size: Int get() = backingMap.size

    /** 是否为空 */
    fun isEmpty(): Boolean = backingMap.isEmpty()

    /** 清空所有内容 */
    fun clear() = backingMap.clear()

    /** 查找所有a=指定值的(b,c,value)集合 */
    fun getByFirstKey(a: A): List<Triple<B, C, V>> =
        backingMap.filterKeys { it.first == a }.map { Triple(it.key.second, it.key.third, it.value) }

    /** 查找所有b=指定值的(a,c,value)集合 */
    fun getBySecondKey(b: B): List<Triple<A, C, V>> =
        backingMap.filterKeys { it.second == b }.map { Triple(it.key.first, it.key.third, it.value) }

    /** 查找所有c=指定值的(a,b,value)集合 */
    fun getByThirdKey(c: C): List<Triple<A, B, V>> =
        backingMap.filterKeys { it.third == c }.map { Triple(it.key.first, it.key.second, it.value) }

    /** 克隆 */
    public override fun clone(): TripleKeyMap<A, B, C, V> {
        val copy = TripleKeyMap<A, B, C, V>()
        copy.backingMap.putAll(this.backingMap)
        return copy
    }

    /** 遍历所有三键值对 */
    fun forEach(action: (A, B, C, V) -> Unit) {
        backingMap.forEach { (triple, v) -> action(triple.first, triple.second, triple.third, v) }
    }

    /** 以 (Triple<A,B,C>,V) 的形式遍历 */
    operator fun iterator(): Iterator<Map.Entry<Triple<A, B, C>, V>> = backingMap.entries.iterator()

    override fun toString(): String = backingMap.toString()
}
