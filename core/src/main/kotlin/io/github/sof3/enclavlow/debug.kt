package io.github.sof3.enclavlow

var IS_DEBUG = true

object DebugOutput {
    private val EPOCH = System.nanoTime()
    private var counter = 0
    private val tags = mutableListOf<String>()

    private fun getIndent() = tags.lastOrNull() ?: ""

    fun pushTag(tag: String) {
        print(getIndent())
        println("{$tag}")
        tags.add("  ".repeat(tags.size + 1))
    }

    fun popTag() {
        tags.removeLast()
    }

    fun put(message: String) {
        if (!IS_DEBUG) return

        if (counter > 10000) throw Error("Too many debug messages; possible infinite loop?")

        for (line in message.split("\n")) {
            print(getIndent())
            print("[%+.3f] ".format((System.nanoTime() - EPOCH) / 1e6f))
            println(line)
        }

        counter++
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