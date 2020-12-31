package io.github.sof3.enclavlow.local

import io.github.sof3.enclavlow.contract.ControlNode
import io.github.sof3.enclavlow.contract.ExplicitSinkLocalNode
import io.github.sof3.enclavlow.contract.ExplicitSourceLocalNode
import io.github.sof3.enclavlow.contract.LocalControlNode
import io.github.sof3.enclavlow.contract.LocalNode
import io.github.sof3.enclavlow.contract.LocalVarNode
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ProjectionNode
import io.github.sof3.enclavlow.contract.ProxyLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.StaticLocalNode
import io.github.sof3.enclavlow.contract.ThisLocalNode
import io.github.sof3.enclavlow.contract.ThrowLocalNode
import io.github.sof3.enclavlow.util.Edge
import io.github.sof3.enclavlow.util.MutableDenseGraph
import io.github.sof3.enclavlow.util.getOrFill
import io.github.sof3.enclavlow.util.indexedSetOf
import io.github.sof3.enclavlow.util.newDenseGraph
import soot.SootField
import soot.SootMethod

fun newLocalFlow(paramCount: Int, control: ControlNode, flow: SenFlow): LocalFlow {
    val params = List(paramCount) { ParamLocalNode(it) }
    val graph = makeLocalFlowGraph(params + (control as LocalNode))
    return LocalFlow(graph, control, mutableMapOf(), mutableSetOf(), mutableSetOf(), params, flow)
}

class LocalFlow(
    val graph: LocalFlowGraph,
    _control: LocalNode,
    var locals: MutableMap<String, LocalVarNode>,
    var calls: MutableSet<FnCall>,
    var projections: MutableSet<ProjectionNode<*>>,
    var params: List<ParamLocalNode>,
    private val flow: SenFlow,
) {
    var control: LocalNode = _control
        set(value) {
            if (value !is ControlNode) throw ClassCastException("control must be ControlNode")
            field = value
        }
    var finalizers = ArrayDeque<(LocalFlow) -> Unit>()

    fun getOrAddLocal(name: String): LocalVarNode {
        val local = locals.getOrFill(name) { LocalVarNode(name) }
        graph.addNodeIfMissing(local)
        return local
    }

    fun getLocal(name: String): LocalVarNode? {
        val local = locals[name] ?: return null
        graph.addNodeIfMissing(local)
        return local
    }

    fun addCall(call: FnCall) {
        calls.add(call)
        flow.outputContract.calls.add(call)
    }

    fun getProjection(base: LocalNode, fieldDecl: String): ProjectionNode<LocalNode> {
        val fieldNode = ProjectionNode.create(base, fieldDecl)
        fieldNode as LocalNode
        graph.addNodeIfMissing(fieldNode)
        if (fieldNode !in projections) {
            projections.add(fieldNode)
            // graph.touch(base, fieldNode) { causes += LocalFlowCause.FIELD_PROJECTION }
            graph.touch(fieldNode, base) { causes += LocalFlowCause.FIELD_PROJECTION_BACK_FLOW }

            for ((original, edge) in graph.flowsToEdges(base)) {
                if ((edge.causes intersect listOf(
                        LocalFlowCause.ASSIGNMENT,
                        LocalFlowCause.FIELD_ASSIGNMENT,
                        LocalFlowCause.FIELD_ASSIGNMENT_BACK_FLOW)
                        ).isNotEmpty()) {
                    val originalField = getProjection(original, fieldDecl) as LocalNode
                    graph.touch(originalField, fieldNode) {
                        causes += LocalFlowCause.FIELD_ASSIGNMENT
                    }
                    graph.touch(fieldNode, originalField) { causes += LocalFlowCause.FIELD_ASSIGNMENT_BACK_FLOW }
                }
            }
        }
        return fieldNode
    }

    fun getProjectionAsNode(base: LocalNode, field: SootField) = getProjection(base, field.declaration) as LocalNode
    fun getUnknownOffsetProjectionAsNode(base: LocalNode) = getProjection(base, "<unknown offset>") as LocalNode

    fun finalizedCopy(methodControl: MethodControlNode): LocalFlow {
        val copy = newLocalFlow(params.size, control as ControlNode, flow)
        this copyTo copy
        copy.graph.addNodeIfMissing(methodControl)
        for (node in copy.graph.nodes) {
            if (node is LocalControlNode) {
                copy.graph.touch(methodControl, node) { causes += LocalFlowCause.METHOD_CONTROL }
            }
        }
//        for (f in finalizers) f(copy)
        return copy
    }

    infix fun copyTo(dest: LocalFlow) {
        graph copyTo dest.graph
        dest.locals = locals
        dest.calls = calls
        dest.params = params
        dest.control = control
//        dest.control = ControlNode() // construct a new instance in case it's overwritten (?)
//        dest.graph.addNodeIfMissing(dest.control)
//
//        // edges on both sides, because we want to equalize
//        dest.graph.touch(control, dest.control, "Flow copy\\nForward") {
//            copyFlow = true
//        }
////        dest.graph.touch(dest.control, control, "Flow copy\\nBackward")
//        dest.finalizers = ArrayDeque(finalizers)
        dest.projections = projections
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LocalFlow) return false
        if (graph.filterNodes { it !is ControlNode } != other.graph.filterNodes { it !is ControlNode }) return false
        return params == other.params && locals == other.locals && calls == other.calls
    }

    override fun hashCode() = throw UnsupportedOperationException("LocalFlow is not hashable")

    override fun toString() = "LocalFlow(control=$control, locals=$locals, params=$params, calls=$calls, graph=$graph)"
}

typealias LocalFlowGraph = MutableDenseGraph<LocalNode, LocalEdge>

fun makeLocalFlowGraph(vararg extraNodes: Iterable<LocalNode>): LocalFlowGraph {
    val nodes = indexedSetOf<LocalNode>(ThisLocalNode, StaticLocalNode, ReturnLocalNode, ThrowLocalNode, ExplicitSourceLocalNode, ExplicitSinkLocalNode)
    for (extra in extraNodes) {
        nodes.addAll(extra)
    }

    return newDenseGraph(nodes) { LocalEdge(mutableSetOf()) }
}

data class LocalEdge(val causes: MutableSet<LocalFlowCause>) : Edge<LocalEdge, LocalNode> {
    override fun mergeEdge(other: LocalEdge) = LocalEdge((causes + other.causes).toMutableSet())

    override fun graphEqualsImpl(other: Any): Boolean {
        if (other !is LocalEdge) return false
        if (causes != other.causes) return false
        return true
    }

    override fun getGraphvizAttributes(from: LocalNode, to: LocalNode): Iterable<Pair<String, String>> {
        return listOf(
            "label" to causes.joinToString(",\\n") { it.label.replace("\n", "\\n") },
            "style" to if (causes.any { it.refOnly }) "dotted" else "solid",
            "color" to if (causes.any { it.projectionBackFlow }) "blue" else "black",
        )
    }
}

enum class LocalFlowCause(val label: String, val refOnly: Boolean = false, var projectionBackFlow: Boolean = false) {
    FIELD_PROJECTION("field projection"),
    FIELD_PROJECTION_BACK_FLOW("field projection backflow", projectionBackFlow = true),
    METHOD_CONTROL("method control"),
    FLOW_MERGE("flow merge"),
    FLOW_MERGE_HACK("flow merge\nhack"),
    BRANCH("branch"),
    ASSIGNMENT("assignment"),
    ASSIGNMENT_SIDE_EFFECT("assignment\nside effect"),
    ASSIGNMENT_CONDITION("assignment\ncondition"),
    FIELD_ASSIGNMENT("field assignment"),
    FIELD_ASSIGNMENT_BACK_FLOW("field assignment backflow"),

    /**
     * Projections in the source are propagated to projections in the destination
     */
    REF_BACK_FLOW("reference\nback flow", refOnly = true),
    SINK_MARKER("sink marker"),
    CALL_PARAM("call param"),
    CALL_CONTEXT("call context"),
    CALL_CONDITION("call condition"),
}

data class FnIden(
    val clazz: String,
    val method: String,
) {
    constructor(soot: SootMethod) : this(soot.declaringClass.name, soot.subSignature)

    override fun toString() = "$clazz.$method"
}

data class FnCall(
    val fn: FnIden,
    val params: List<ProxyLocalNode>,
    val thisNode: ProxyLocalNode?,
    val returnNode: ProxyLocalNode,
    val throwNode: ProxyLocalNode,
    val controlNode: ProxyLocalNode,
) {
    fun allNodes() = sequence {
        yieldAll(params)
        thisNode?.let { yield(it) }
        yield(returnNode)
        yield(throwNode)
        yield(controlNode)
    }

    override fun toString() = "FnCall(fn=$fn)"
}

fun createFnCall(method: SootMethod): FnCall {
    val fnIden = FnIden(method)
    val fn = fnIden.toString()
    val params = List(method.parameterCount) { ProxyLocalNode("<$fn>\nparam $it") }
    val thisNode = if (method.isStatic) null else ProxyLocalNode("<$fn>\nthis")
    val returnNode = ProxyLocalNode("<$fn>\nreturn")
    val throwNode = ProxyLocalNode("<$fn>\nthrow")
    val controlNode = ProxyLocalNode("<$fn>\ncontrol")
    return FnCall(fnIden, params, thisNode, returnNode, throwNode, controlNode)
}
