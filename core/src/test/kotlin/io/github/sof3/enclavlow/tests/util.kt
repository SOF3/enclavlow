package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.Contract
import io.github.sof3.enclavlow.SenTransformer
import io.github.sof3.enclavlow.cases.BinomLeak
import soot.Scene
import soot.SootClass
import soot.options.Options
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.locks.ReentrantLock
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val disableConcurrency = ReentrantLock()

internal inline fun <reified T> run(vararg results: Pair<String, Contract>) = runImpl(T::class.java, results)
private fun <T> runImpl(clazz: Class<T>, results: Array<out Pair<String, Contract>>) {
    disableConcurrency.lock()
    try {
        val options = Options.v()

        options.set_output_format(Options.output_format_jimple)
        options.set_output_dir("build/sootOutput")

        val classPath = ManagementFactory.getRuntimeMXBean().classPath
            .split(":")
            .filter { File(it).exists() }
            .joinToString(separator = ":")
        options.set_soot_classpath("${Scene.defaultJavaClassPath()}:$classPath") // cp of current JVM runtime

        println(clazz.name)
//        Scene.v().addBasicClass(clazz.name, SootClass.SIGNATURES)

        SenTransformer.contracts.clear()
        soot.Main.v().run(arrayOf(clazz.name)) // don't use Main.main(), otherwise it will exit directly

        for (entry in results) {
            assertTrue("Method ${entry.first} was not analyzed, only got ${SenTransformer.contracts.keys}") {
                entry.first in SenTransformer.contracts
            }
            assertEquals(entry.second, SenTransformer.contracts[entry.first], "Method ${entry.first} has unexpected contract")
        }
    } finally {
        disableConcurrency.unlock()
        try {
            soot.Timers.v().totalTimer.end()
        } catch (e: RuntimeException) {
            /* no-op */
        }
    }
}
