package io.github.sof3.enclavlow.util

var IS_DEBUG = false
var ENCLAVLOW_DEBUG_LOGGER: (String) -> Unit = { println(it) }

object DebugOutput {
    val EPOCH = System.nanoTime()
    var counter = 0
    private val tags = mutableListOf<String>()

    fun getIndent() = tags.lastOrNull() ?: ""

    fun pushTag(tag: String) = synchronized(this) {
        if (!IS_DEBUG) return@synchronized
        ENCLAVLOW_DEBUG_LOGGER(getIndent() + tag)
        tags.add("  ".repeat(tags.size + 1))
        return@synchronized
    }

    fun popTag() = synchronized(this) {
        if (!IS_DEBUG) return@synchronized
        tags.removeLast()
        return@synchronized
    }

    inline fun put(message: () -> String) = synchronized(this) {
        if (!IS_DEBUG) return

        if (counter > 10000) throw Error("Too many debug messages; possible infinite loop?")

        val time = "[${Thread.currentThread().id} %+.3f] ".format((System.nanoTime() - EPOCH) / 1e6f)
        for ((i, line) in message().split("\n").withIndex()) {
            val sb = StringBuilder(getIndent())
            if (i == 0) {
                sb.append(time)
            } else {
                sb.append(" ".repeat(time.length))
            }
            sb.append(line)
            ENCLAVLOW_DEBUG_LOGGER(sb.toString())
        }

        counter++

        return@synchronized
    }
}

inline fun printDebug(message: () -> Any?) = DebugOutput.put { message().toString() }

inline fun block(tag: String, fn: () -> Unit) {
    DebugOutput.pushTag(tag)
    try {
        fn()
    } finally {
        DebugOutput.popTag()
    }
}
