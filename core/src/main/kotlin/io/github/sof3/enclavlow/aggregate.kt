package io.github.sof3.enclavlow

import java.lang.StringBuilder

fun computeAggregate(classpath: List<String>, entryClasses: List<String>) {
    soot.G.reset()

    val optionsConfig: soot.options.Options.() -> Unit = {
        set_prepend_classpath(true)
        set_soot_classpath(classpath.joinToString(separator = ":"))
    }
    soot.options.Options.v().apply(optionsConfig)
    val scene = soot.Scene.v()
    scene.loadNecessaryClasses()

    val initial = mutableSetOf<Pair<String, String>>()
    for (className in entryClasses) {
        val clazz = scene.loadClassAndSupport(className)
        for (method in clazz.methodIterator()) {
            initial.add(className to method.subSignature)
        }
    }

    var requests = initial.takeIf { it.isNotEmpty() }
    val contracts = mutableMapOf<Pair<String, String>, Contract<out ContractFlowGraph>>()

    while (requests != null) {
        val swap = mutableSetOf<Pair<String, String>>()
        for (request in requests) {
            val contract = analyzeMethod(request.first, request.second, MethodNameType.SUB_SIGNATURE, optionsConfig)
            contracts[request] = contract
            for (call in contract.calls) {
                val iden = call.iden.declClass to call.iden.subSig
                if (iden !in contracts && iden !in requests) {
                    swap.add(iden)
                }
            }
        }
        requests = swap.takeIf { it.isNotEmpty() }
    }

    val aggregate = newDiGraph<ContractNode, ContractEdge> { ContractEdge() }
}
