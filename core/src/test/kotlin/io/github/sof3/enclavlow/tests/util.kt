package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.contract.Contract
import io.github.sof3.enclavlow.contract.ContractFlowGraph
import io.github.sof3.enclavlow.contract.MethodNameType
import io.github.sof3.enclavlow.contract.analyzeMethod
import io.github.sof3.enclavlow.local.PRINT_DOT
import io.github.sof3.enclavlow.util.IS_DEBUG
import io.github.sof3.enclavlow.util.MutableDenseGraph
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.test.assertEquals

private val allRuns = hashMapOf<Class<*>, Map<Pair<String, String>, Contract<out ContractFlowGraph>>>()

internal inline fun <reified T> testMethod(method: String, contract: Contract<out ContractFlowGraph>): Unit =
    runImpl(T::class.java, method, contract)

private fun <T> runImpl(clazz: Class<T>, method: String, expectedContract: Contract<out ContractFlowGraph>) = synchronized(allRuns) {
    IS_DEBUG = true
    PRINT_DOT = true

    val actual = analyzeMethod(clazz.name, method, MethodNameType.UNIQUE_NAME) {
        set_output_dir("build/sootOutput")
        val classPath = ManagementFactory.getRuntimeMXBean().classPath
            .split(":")
            .filter { File(it).exists() }
            .joinToString(separator = ":")
        set_prepend_classpath(true)
        set_soot_classpath(classPath) // cp of current JVM runtime
    }

    (expectedContract.graph as MutableDenseGraph).sortNodes { it.toString() }
    (actual.graph as MutableDenseGraph).sortNodes { it.toString() }
    assertEquals(expectedContract.graph, actual.graph, "Method $method has unexpected contract")
    assertEquals(expectedContract.callTags, actual.callTags, "Method $method has unexpected contract")
}
