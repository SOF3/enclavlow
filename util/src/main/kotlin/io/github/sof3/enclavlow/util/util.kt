package io.github.sof3.enclavlow.util

import kotlin.random.Random

fun <T : Any> indexedSetOf(vararg values: T): IndexedSet<T> {
    val set = IndexedSet<T>()
    for (value in values) {
        set.addIfMissing(value)
    }
    return set
}

class IndexedSet<T : Any> internal constructor(
    private val values: MutableList<T> = mutableListOf(),
    private val index: MutableMap<T, Int> = mutableMapOf(),
) : Iterable<T>, Cloneable {
    fun addIfMissing(value: T): Boolean {
        if (index.containsKey(value)) return false
        values.add(value)
        index[value] = values.lastIndex
        return true
    }

    fun addAll(newValues: Iterable<T>) {
        var i = values.size
        for (value in newValues) {
            index[value] = i
            i += 1
        }
        values.addAll(newValues)
    }

    operator fun contains(value: T) = index.containsKey(value)

    operator fun get(i: Int) = values[i]

    val size get() = values.size

    fun find(value: T) = index[value]

    override fun iterator() = values.iterator()

    override fun equals(other: Any?): Boolean {
        if (other !is IndexedSet<*>) return false
        for (value in values) {
            if (!other.index.containsKey(value)) return false
        }
        for (value in other.values) {
            if (!index.containsKey(value)) return false
        }
        return true
    }

    override fun hashCode() = throw NotImplementedError()

    override fun toString() = "{$values}"

    public override fun clone() = IndexedSet(values.toMutableList(), index.toMutableMap())
}

inline fun <K, V> MutableMap<K, V>.getOrFill(k: K, fill: () -> V): V {
    val get = this[k]
    if (get != null) return get
    val value = fill()
    this[k] = value
    return value
}

inline fun alwaysAssert(cond: Boolean, message: () -> String) {
    if (!cond) throw AssertionError(message())
}

fun <T> randomColor(t: T): String {
    val random = Random(t.hashCode())
    val sb = StringBuilder(7)
    sb.append('#')
    for (i in 0 until 6) sb.append(random.nextInt(16).toString(16))
    return sb.toString()
}
