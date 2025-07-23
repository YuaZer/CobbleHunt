package io.github.yuazer.cobblehunt.utils.extension

import kotlin.random.Random

object CollectionExtension {
    /**
     * 从集合中随机抽取 n 个元素，如果数量不足则全部返回。
     */
    fun <T> Collection<T>.randomSample(n: Int): List<T> {
        return if (this.size <= n) {
            this.toList()
        } else {
            this.shuffled(Random(System.nanoTime())).take(n)
        }
    }
}