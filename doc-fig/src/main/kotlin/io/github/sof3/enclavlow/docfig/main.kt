package io.github.sof3.enclavlow.docfig

import io.github.sof3.enclavlow.IS_DEBUG
import io.github.sof3.enclavlow.MethodNameType
import io.github.sof3.enclavlow.analyzeMethod
import java.io.File
import java.lang.management.ManagementFactory

fun main() {
    IS_DEBUG = false
    val contract = analyzeMethod("TraditionalFalsePositive", "foo", MethodNameType.UNIQUE_NAME) {
        set_output_dir("build/sootOutput")
        val classPath = ManagementFactory.getRuntimeMXBean().classPath
            .split(":")
            .filter { File(it).exists() }
            .joinToString(separator = ":")
        set_prepend_classpath(true)
        set_soot_classpath(classPath) // cp of current JVM runtime
    }

    val file = File("build/TraditionalFalsePositive.dot")
    file.writeText(contract.graph.toGraphviz("graph", listOf("rankDir" to "LR")))
}
