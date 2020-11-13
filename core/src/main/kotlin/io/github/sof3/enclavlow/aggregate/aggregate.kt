package io.github.sof3.enclavlow.aggregate

import io.github.sof3.enclavlow.contract.Contract
import io.github.sof3.enclavlow.contract.ContractFlowGraph
import io.github.sof3.enclavlow.contract.ContractNode
import io.github.sof3.enclavlow.contract.ContractProjectionNode
import io.github.sof3.enclavlow.contract.ExplicitSinkLocalNode
import io.github.sof3.enclavlow.contract.ExplicitSourceLocalNode
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.MethodNameType
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ProxyLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.ScopedContractNode
import io.github.sof3.enclavlow.contract.StaticLocalNode
import io.github.sof3.enclavlow.contract.ThisLocalNode
import io.github.sof3.enclavlow.contract.ThrowLocalNode
import io.github.sof3.enclavlow.contract.analyzeMethod
import io.github.sof3.enclavlow.local.FnIden
import io.github.sof3.enclavlow.util.MutableDiGraph
import io.github.sof3.enclavlow.util.newDiGraph

fun computeAggregate(classpath: List<String>, entryClasses: List<String>): MutableDiGraph<AggNode, AggEdge> {
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
                val fn = FnIden(call.fn.clazz, call.fn.method)
                if (fn !in contracts && fn !in requests) {
                    swap.add(fn)
                }
            }
        }
        requests = swap.takeIf { it.isNotEmpty() }
    }

    val aggregate = newDiGraph { AggEdge() }

    for ((fn, contract) in contracts) {
        val proxyMap = mutableMapOf<ProxyLocalNode, FnAggNode>()
        for (call in contract.calls) {
            // just construct a new object; FnAggNode will delegate equality and hashCode
            proxyMap[call.controlNode] = FnAggNode(call.fn, MethodControlNode)
            proxyMap[call.returnNode] = FnAggNode(call.fn, ReturnLocalNode)
            proxyMap[call.throwNode] = FnAggNode(call.fn, ThrowLocalNode)
            val thisNode = call.thisNode
            if (thisNode != null) {
                proxyMap[thisNode] = FnAggNode(call.fn, ThisLocalNode)
            }
            for ((i, param) in call.params.withIndex()) {
                proxyMap[param] = FnAggNode(call.fn, ParamLocalNode(i))
            }
        }

        contract.graph.forEachEdge { from, to, _ ->
            val left = contractNodeToAgg(fn, from, proxyMap)
            val right = contractNodeToAgg(fn, to, proxyMap)
            aggregate.addNodeIfMissing(left)
            aggregate.addNodeIfMissing(right)
            aggregate.touch(left, right) {}
        }
    }

    return aggregate
}

private fun contractNodeToAgg(fn: FnIden, node: ContractNode, proxyMap: Map<ProxyLocalNode, FnAggNode>): AggNode = when (node) {
    is ScopedContractNode -> FnAggNode(fn, node)
    is ExplicitSourceLocalNode -> ExplicitSourceAggNode
    is ExplicitSinkLocalNode -> ExplicitSinkAggNode
    is StaticLocalNode -> StaticAggNode
    is ContractProjectionNode -> TODO()
    is ProxyLocalNode -> proxyMap[node] ?: throw IllegalArgumentException("Unknown ProxyLocalNode")
}
