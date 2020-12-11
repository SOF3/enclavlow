package io.github.sof3.enclavlow.plugin

import io.github.sof3.enclavlow.aggregate.AggGraph
import io.github.sof3.enclavlow.aggregate.AggNode
import io.github.sof3.enclavlow.aggregate.ExplicitSourceAggNode
import io.github.sof3.enclavlow.aggregate.computeAggregate
import io.github.sof3.enclavlow.util.ENCLAVLOW_DEBUG_LOGGER
import io.github.sof3.enclavlow.util.IS_DEBUG
import io.github.sof3.enclavlow.util.printDebug
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPluginConvention
import org.slf4j.LoggerFactory
import java.io.FileWriter
import java.util.*

private val logger = LoggerFactory.getLogger(Main::class.java)

class Main : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.create("enclavlow") { task ->
            task.dependsOn("classes")
            task.doLast {
                IS_DEBUG = project.gradle.startParameter.logLevel == LogLevel.DEBUG
                val writer by lazy { FileWriter("plugin.log") }
                ENCLAVLOW_DEBUG_LOGGER = {
                    writer.append(it).append('\n')
                    logger.debug(it)
                }

                val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
                val classpath = convention.sourceSets.getByName("main").runtimeClasspath.files
                val entryPath = convention.sourceSets.getByName("main").output.classesDirs.files

                val entryClasses = mutableListOf<String>()
                for (base in entryPath) {
                    for (file in base.walkTopDown().asIterable()) {
                        if (file.extension == "class") {
                            val pk = file.parentFile.relativeTo(base)
                            val name = pk.path.replace('/', '.') + "." + file.nameWithoutExtension
                            entryClasses.add(name)
                        }
                    }
                }

                logger.info("Computing AFG from $entryClasses")

                val (afg, crossEdges) = computeAggregate(classpath.map { it.absolutePath }, entryClasses)

                afg.addNodeIfMissing(ExplicitSourceAggNode)

                class Link(val node: AggNode, val prev: Link? = null) {
                    private val depth: Int = (prev?.depth ?: 0) + 1

                    fun asList(): List<AggNode> {
                        val list = ArrayList<AggNode>(depth)
                        appendToList(list)
                        return list
                    }

                    private fun appendToList(list: ArrayList<AggNode>) {
                        prev?.appendToList(list)
                        list.add(node)
                    }
                }

                val leakPaths = mutableListOf<List<AggNode>>()
                val visited = mutableSetOf<AggNode>()
                fun dfs(graph: AggGraph, src: AggNode, path: Link) {
                    if (src in visited) return
                    visited.add(src)
                    for ((dest, edge) in graph.flowsFromEdges(src)) {
                        val link = Link(dest, path)
                        if ((src to dest) in crossEdges) {
                            leakPaths.add(link.asList())
                            edge.leak = true
                        }
                        dfs(graph, dest, link)
                    }
                }

                dfs(afg, ExplicitSourceAggNode, Link(ExplicitSourceAggNode, null))
            }
        }
    }
}
