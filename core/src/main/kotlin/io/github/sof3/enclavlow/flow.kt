package io.github.sof3.enclavlow

typealias Contract = DiGraph<PublicNode>
typealias MutableContract = MutableDiGraph<PublicNode>
typealias FlowSet = MutableDiGraph<Node>

fun makeContract(
    paramCount: Int,
    extraNodes: Collection<PublicNode> = emptyList(),
    fn: MakeContractContext<PublicNode>.() -> Unit = {},
): MutableContract {
    val nodes = indexedSetOf(ThisScope, StaticScope, ReturnScope, ThrowScope)
    nodes.addAll((0 until paramCount).map { ParamNode(it) })
    nodes.addAll(extraNodes)

    val graph = newDiGraph(nodes)
    fn(MakeContractContext(graph))
    return graph
}

fun makeFlowSet(vararg extraNodes: Iterable<Node>): FlowSet {
    val nodes = indexedSetOf<Node>(ThisScope, StaticScope, ReturnScope, ThrowScope)
    for (extra in extraNodes) {
        nodes.addAll(extra)
    }

    return newDiGraph(nodes)
}

class MakeContractContext<T : Any>(private val graph: MutableDiGraph<T>) {
    infix fun T.into(other: T) {
        graph.touch(this, other)
    }
}
