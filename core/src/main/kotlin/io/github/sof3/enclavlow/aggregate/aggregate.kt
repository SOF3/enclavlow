package io.github.sof3.enclavlow.aggregate

import io.github.sof3.enclavlow.contract.Contract
import io.github.sof3.enclavlow.contract.ContractEdge
import io.github.sof3.enclavlow.contract.ContractEdgeType
import io.github.sof3.enclavlow.contract.ContractFlowGraph
import io.github.sof3.enclavlow.contract.ContractNode
import io.github.sof3.enclavlow.contract.MethodNameType
import io.github.sof3.enclavlow.contract.analyzeMethod
import io.github.sof3.enclavlow.local.FnIden
import io.github.sof3.enclavlow.newDiGraph

fun computeAggregate(classpath: List<String>, entryClasses: List<String>) {
    soot.G.reset()

    val optionsConfig: soot.options.Options.() -> Unit = {
        set_prepend_classpath(true)
        set_soot_classpath(classpath.joinToString(separator = ":"))
    }
    soot.options.Options.v().apply(optionsConfig)
    val scene = soot.Scene.v()
    scene.loadNecessaryClasses()

    val initial = mutableSetOf<FnIden>()
    for (className in entryClasses) {
        val clazz = scene.loadClassAndSupport(className)
        for (method in clazz.methodIterator()) {
            initial.add(FnIden(className, method.subSignature))
        }
    }

    var requests = initial.takeIf { it.isNotEmpty() }
    val contracts = mutableMapOf<FnIden, Contract<out ContractFlowGraph>>()

    while (requests != null) {
        val swap = mutableSetOf<FnIden>()
        for (request in requests) {
            val contract = analyzeMethod(request.clazz, request.method, MethodNameType.SUB_SIGNATURE, optionsConfig)
            contracts[request] = contract
            for (call in contract.calls) {
                val iden = FnIden(call.iden.clazz, call.iden.method)
                if (iden !in contracts && iden !in requests) {
                    swap.add(iden)
                }
            }
        }
        requests = swap.takeIf { it.isNotEmpty() }
    }

    val aggregate = newDiGraph<ContractNode, ContractEdge> { ContractEdge(ContractEdgeType.INVOKE) }
}
