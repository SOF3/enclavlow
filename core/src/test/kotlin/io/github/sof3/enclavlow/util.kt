package io.github.sof3.enclavlow

import soot.PackManager
import soot.Scene
import soot.Transform
import soot.options.Options
import java.io.File
import java.lang.management.ManagementFactory

internal inline fun <reified T> run() {
    val options = Options.v()

    options.set_output_format(Options.output_format_jimple)
    options.set_output_dir("build/sootOutput")

    val classPath = ManagementFactory.getRuntimeMXBean().classPath
        .split(":")
        .filter { File(it).exists() }
        .joinToString(separator = ":")
    options.set_soot_classpath("${Scene.defaultJavaClassPath()}:$classPath") // cp of current JVM runtime

    PackManager.v().getPack("jtp").add(Transform("jtp.sen", SenTransformer))

    val clazz = T::class.java
    soot.Main.v().run(arrayOf(clazz.name)) // don't use Main.main(), otherwise it will exit directly
}
