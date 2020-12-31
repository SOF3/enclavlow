package io.github.sof3.enclavlow.aggregate

import io.github.sof3.enclavlow.contract.ScopedContractNode
import io.github.sof3.enclavlow.local.FnIden
import io.github.sof3.enclavlow.util.Edge

sealed class AggNode

object ExplicitSourceAggNode : AggNode() {
    override fun toString() = "<source>"
}

object ExplicitSinkAggNode : AggNode() {
    override fun toString() = "<sink>"
}

object StaticAggNode : AggNode() {
    override fun toString() = "<static>"
}

data class FnAggNode(val fn: FnIden, val variant: ScopedContractNode) : AggNode() {
    override fun toString() = "<$fn>\n$variant"
}

class AggEdge : Edge<AggEdge, AggNode> {
    var leak = false

    override fun mergeEdge(other: AggEdge) = throw UnsupportedOperationException("AFGs should not be merged")

    override fun graphEqualsImpl(other: Any) = this == other

    override fun getGraphvizAttributes(from: AggNode, to: AggNode): Iterable<Pair<String, String>> {
        return listOf()
    }
}

data class ProjectionAggNode(val base: AggNode, val field: String) : AggNode() {
    override fun toString() = "<$base>.$field"
}
