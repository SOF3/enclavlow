package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.contract.Contract
import io.github.sof3.enclavlow.contract.ContractFlowGraph
import io.github.sof3.enclavlow.contract.MethodNameType
import io.github.sof3.enclavlow.contract.analyzeMethod
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.test.assertEquals

private val allRuns = hashMapOf<Class<*>, Map<Pair<String, String>, Contract<out ContractFlowGraph>>>()

internal inline fun <reified T> testMethod(method: String, contract: Contract<out ContractFlowGraph>): Unit =
    runImpl(T::class.java, method, contract)

private fun <T> runImpl(clazz: Class<T>, method: String, expectedContract: Contract<out ContractFlowGraph>) = synchronized(allRuns) {
    val actual = analyzeMethod(clazz.name, method, MethodNameType.UNIQUE_NAME) {
        set_output_dir("build/sootOutput")
        val classPath = ManagementFactory.getRuntimeMXBean().classPath
            .split(":")
            .filter { File(it).exists() }
            .joinToString(separator = ":")
        set_prepend_classpath(true)
        set_soot_classpath(classPath) // cp of current JVM runtime
    }

    assertEquals(expectedContract, actual, "Method $method has unexpected contract")
}
