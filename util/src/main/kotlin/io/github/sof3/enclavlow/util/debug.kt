package io.github.sof3.enclavlow.util

var IS_DEBUG = false

object DebugOutput {
    private val EPOCH = System.nanoTime()
    private var counter = 0
    private val tags = mutableListOf<String>()

    private fun getIndent() = tags.lastOrNull() ?: ""

    fun pushTag(tag: String) = synchronized(this) {
        if (!IS_DEBUG) return@synchronized
        print(getIndent())
        println("{$tag}")
        tags.add("  ".repeat(tags.size + 1))
        return@synchronized
    }

    fun popTag() = synchronized(this) {
        if (!IS_DEBUG) return@synchronized
        tags.removeLast()
        return@synchronized
    }

    fun put(message: String) = synchronized(this) {
        if (!IS_DEBUG) return

        if (counter > 10000) throw Error("Too many debug messages; possible infinite loop?")

        val time = "[${Thread.currentThread().id} %+.3f] ".format((System.nanoTime() - EPOCH) / 1e6f)
        for ((i, line) in message.split("\n").withIndex()) {
            print(getIndent())
            print(if (i == 0) time else " ".repeat(time.length))
            println(line)
        }

        counter++

        return@synchronized
    }
}

fun printDebug(message: Any?) = DebugOutput.put(message.toString())

inline fun block(tag: String, fn: () -> Unit) {
    DebugOutput.pushTag(tag)
    try {
        fn()
    } finally {
        DebugOutput.popTag()
    }
}
