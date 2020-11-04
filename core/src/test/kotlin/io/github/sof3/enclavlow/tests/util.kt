package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.Contract
import io.github.sof3.enclavlow.ContractFlowGraph
import io.github.sof3.enclavlow.SenTransformer
import soot.Scene
import soot.options.Options
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val allRuns = hashMapOf<Class<*>, Map<Pair<String, String>, Contract<out ContractFlowGraph>>>()

internal inline fun <reified T> testMethod(method: String, contract: Contract<out ContractFlowGraph>): Unit = runImpl(T::class.java, method, contract)
private fun <T> runImpl(clazz: Class<T>, method: String, expectedContract: Contract<out ContractFlowGraph>) = synchronized(allRuns) {
    try {
        if (clazz !in allRuns) {
            val options = Options.v()

            options.set_output_format(Options.output_format_jimple)
            options.set_output_dir("build/sootOutput")

            val classPath = ManagementFactory.getRuntimeMXBean().classPath
                .split(":")
                .filter { File(it).exists() }
                .joinToString(separator = ":")
            options.set_soot_classpath("${Scene.defaultJavaClassPath()}:$classPath") // cp of current JVM runtime

            SenTransformer.contracts.clear()
            soot.Main.v().run(arrayOf(clazz.name)) // don't use Main.main(), otherwise it will exit directly

            allRuns[clazz] = SenTransformer.contracts.toMap()
        }

        val key = clazz.name to method
        val actual = allRuns[clazz]!!
        assertTrue("Method $method was not analyzed, only got ${actual.keys}") {
            key in actual
        }
        assertEquals(expectedContract, actual[key], "Method $method has unexpected contract")
    } finally {
        try {
            soot.Timers.v().totalTimer.end()
        } catch (e: RuntimeException) {
            /* no-op */
        }
    }
}
