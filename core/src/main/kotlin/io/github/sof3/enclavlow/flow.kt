package io.github.sof3.enclavlow

typealias Contract = DiGraph<PublicNode>
typealias MutableContract = MutableDiGraph<PublicNode>
typealias FlowSet = MutableDiGraph<Node>

fun makeContract(
    paramCount: Int,
    extraNodes: Collection<PublicNode> = emptyList(),
    fn: MakeContractContext<PublicNode>.() -> Unit = {},
): MutableContract {
    val nodes = indexedSetOf<PublicNode>(ThisScope, StaticScope, ReturnScope, ThrowScope)
    nodes.addAll((0 until paramCount).map { ParamSource(it) })
    nodes.addAll(extraNodes)

    val graph = newDirGraph(nodes)
    fn(MakeContractContext(graph))
    return graph
}

fun makeFlowSet(
    paramCount: Int,
    extraNodes: Collection<Node> = emptyList(),
    fn: MakeContractContext<Node>.() -> Unit = {},
): FlowSet {
    val nodes = indexedSetOf(ThisScope, StaticScope, ReturnScope, ThrowScope, ControlFlow)
    nodes.addAll((0 until paramCount).map { ParamSource(it) })
    nodes.addAll(extraNodes)

    val graph = newDirGraph(nodes)
    fn(MakeContractContext(graph))
    return graph
}

class MakeContractContext<T : Any>(private val graph: MutableDiGraph<T>) {
    infix fun T.into(other: T) {
        graph.touch(this, other)
    }
}
