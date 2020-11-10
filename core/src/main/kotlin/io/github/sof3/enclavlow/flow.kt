package io.github.sof3.enclavlow

data class Contract<G : ContractFlowGraph>(val graph: G, val callTags: CallTags, val calls: MutableList<FnCall>)

typealias ContractFlowGraph = DiGraph<PublicNode>
typealias MutableContractFlowGraph = MutableDiGraph<PublicNode>
typealias LocalFlowGraph = MutableDiGraph<Node>

fun makeContract(
    callTags: CallTags,
    paramCount: Int,
    extraNodes: Collection<PublicNode> = emptyList(),
    fn: MakeContractContext<PublicNode>.() -> Unit = {},
): Contract<MutableContractFlowGraph> {
    val nodes = indexedSetOf(ThisNode, StaticNode, ReturnNode, ThrowNode, ExplicitSourceNode, ExplicitSinkNode)
    nodes.addAll((0 until paramCount).map { ParamNode(it) })
    nodes.addAll(extraNodes)

    val graph = newDiGraph(nodes)
    fn(MakeContractContext(graph))
    return Contract(graph, callTags, mutableListOf())
}

fun makeLocalFlowGraph(vararg extraNodes: Iterable<Node>): LocalFlowGraph {
    val nodes = indexedSetOf<Node>(ThisNode, StaticNode, ReturnNode, ThrowNode, ExplicitSourceNode, ExplicitSinkNode)
    for (extra in extraNodes) {
        nodes.addAll(extra)
    }

    return newDiGraph(nodes)
}

class MakeContractContext<T : Any>(private val graph: MutableDiGraph<T>) {
    infix fun T.into(other: T) {
        graph.touch(this, other, "construction")
    }
}

enum class CallTags {
    UNSPECIFIED,
    ENCLAVE_CALL,
    OUTSIDE_CALL,
}
