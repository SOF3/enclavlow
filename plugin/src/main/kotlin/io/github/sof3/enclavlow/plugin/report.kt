package io.github.sof3.enclavlow.plugin

import io.github.sof3.enclavlow.aggregate.AggGraph
import io.github.sof3.enclavlow.aggregate.AggNode
import io.github.sof3.enclavlow.contract.Contract
import io.github.sof3.enclavlow.contract.ContractFlowGraph
import io.github.sof3.enclavlow.local.FnIden
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.li
import kotlinx.html.link
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.stream.appendHTML
import kotlinx.html.title
import kotlinx.html.ul
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.net.URLEncoder

internal fun printReport(file: File, graph: AggGraph, leakPaths: MutableList<List<AggNode>>, contracts: MutableMap<FnIden, Contract<out ContractFlowGraph>>, entryClasses: List<String>, visited: MutableSet<Pair<AggNode, AggNode>>) {
    OutputStreamWriter(FileOutputStream(file)).use {
        it.write("<!DOCTYPE HTML>\n")
        it.appendHTML().html {
            head {
                title("Enclave analysis report")
                link(href = "enclavlow.css", rel = "stylesheet", type = "text/css")
            }

            body {
                h1 {
                    +"Enclave analysis report"
                }
                p {
                    +"${contracts.size} functions analyzed."
                }

                h2 {
                    +"Sensitive leaks"
                }
                p {
                    +"Sensitive data leaked in ${leakPaths.size} paths."
                }
                if (leakPaths.isNotEmpty()) {
                    div(classes = "leak-paths") {
                        for (leakPath in leakPaths) {
                            div(classes = "leak-path") {
                                for (item in leakPath) {
                                    span(classes = "leak-component") {
                                        span(classes = "node") {
                                            +item.toString()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                h2 {
                    +"Sensitive paths"
                }
                ul(classes = "sen-paths") {
                    for ((src, dest) in visited) {
                        li(classes = "sen-edge") {
                            span(classes = "node sen-src") {
                                +src.toString()
                            }
                            span(classes = "node sen-dest") {
                                +dest.toString()
                            }
                        }
                    }
                }

                h2 {
                    +"Method contracts"
                }
                for (clazz in entryClasses) {
                    h3 { +clazz }
                    ul {
                        for ((fn, contract) in contracts) {
                            if (fn.clazz == clazz) {
                                val graphviz = contract.graph.toGraphviz("flow")
                                val url = URLEncoder.encode(graphviz, "utf-8").replace("+", "%20")
                                li {
                                    a(href = "https://dreampuf.github.io/GraphvizOnline/#$url", target = "_blank") {
                                        +"${fn.method} (view contract via dreampuf graphviz)"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    OutputStreamWriter(FileOutputStream(File(file.parentFile, "enclavlow.css"))).use {
        it.write(".leak-path { margin-left: 2em;}")
        it.write(".node { margin: 1em; border: solid 1pt; }")
        it.write(".leak-component:not(:first-child)::before { content: \"→ \"; }")
    }
}
