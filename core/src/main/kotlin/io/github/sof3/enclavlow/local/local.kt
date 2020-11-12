package io.github.sof3.enclavlow.local

import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.Contract
import io.github.sof3.enclavlow.contract.ContractNode
import io.github.sof3.enclavlow.contract.ExplicitSinkLocalNode
import io.github.sof3.enclavlow.contract.ExplicitSourceLocalNode
import io.github.sof3.enclavlow.contract.LocalControlNode
import io.github.sof3.enclavlow.contract.LocalNode
import io.github.sof3.enclavlow.contract.LocalVarNode
import io.github.sof3.enclavlow.contract.MutableContractFlowGraph
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ProxyLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.StaticLocalNode
import io.github.sof3.enclavlow.contract.ThisLocalNode
import io.github.sof3.enclavlow.contract.ThrowLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.util.Edge
import io.github.sof3.enclavlow.util.MutableDiGraph
import io.github.sof3.enclavlow.util.alwaysAssert
import io.github.sof3.enclavlow.util.block
import io.github.sof3.enclavlow.util.getOrFill
import io.github.sof3.enclavlow.util.indexedSetOf
import io.github.sof3.enclavlow.util.newDiGraph
import io.github.sof3.enclavlow.util.notNull
import io.github.sof3.enclavlow.util.printDebug
import soot.SootMethod
import soot.Value
import soot.jimple.BreakpointStmt
import soot.jimple.DefinitionStmt
import soot.jimple.GotoStmt
import soot.jimple.IfStmt
import soot.jimple.InvokeExpr
import soot.jimple.InvokeStmt
import soot.jimple.MonitorStmt
import soot.jimple.NopStmt
import soot.jimple.PlaceholderStmt
import soot.jimple.ReturnStmt
import soot.jimple.ReturnVoidStmt
import soot.jimple.SwitchStmt
import soot.jimple.ThrowStmt
import soot.toolkits.graph.UnitGraph
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis

class SenFlow(
    graph: UnitGraph,
    private val paramCount: Int,
    callTags: CallTags,
) : ForwardBranchedFlowAnalysis<LocalFlow>(graph) {
    val outputContract: Contract<MutableContractFlowGraph> = makeContract(callTags, paramCount)

    override fun newInitialFlow() = newLocalFlow(paramCount)
    override fun merge(in1: LocalFlow, in2: LocalFlow, out: LocalFlow) = block("Merge") {
        printDebug("in1: $in1")
        printDebug("in2: $in2")
        printDebug("out: ${out.control}")

        in1.graph.merge(in2.graph) { a, b ->
            // TODO handle ControlFlow
            // if flow is detected from either side
            if (a != null) {
                if (b != null) {
                    a.mergeEdge(b)
                } else {
                    a
                }
            } else {
                b
            }
        } copyTo out.graph
        out.graph.addNodeIfMissing(out.control) // we removed it during graph merge

        out.locals = mutableMapOf<String, LocalVarNode>().apply {
            putAll(in1.locals)
            putAll(in2.locals)
        }
        out.calls = mutableListOf<FnCall>().apply {
            addAll(in1.calls)
            addAll(in2.calls)
        }

        out.finalizers.addAll(in1.finalizers union in2.finalizers)

        val merge1 = in1.control
        val merge2 = in2.control
        val mergeTarget = out.control
        out.finalizers.addFirst { final ->
            val lca = final.graph.lca(merge1, merge2, { it is LocalControlNode }, { it.copyFlow })
            if (lca != null) {
                final.graph.touch(lca, mergeTarget) { causes += "Flow merge" }
            } else {
                final.graph.touch(merge1, mergeTarget) { causes += "Flow merge\\nHack" }
                final.graph.touch(merge2, mergeTarget) { causes += "Flow merge\\nHack" }
            }
        }
    }

    override fun copy(source: LocalFlow, dest: LocalFlow) = block("Copy") { source copyTo dest }

    public override fun doAnalysis() = super.doAnalysis()

    override fun flowThrough(
        input: LocalFlow,
        stmt: soot.Unit,
        fallOutList: List<LocalFlow>,
        branchOutList: List<LocalFlow>,
    ) = block("Node $stmt") {
        printDebug("${stmt.javaClass.simpleName}: $stmt")
        printDebug("Input: {$input}")
        val output = fallOutList.getOrNull(0)
        if (fallOutList.size > 1) throw java.lang.AssertionError("Unsupported fallOutList non-singleton")

        when (stmt) {
            is ReturnStmt -> {
                val final = input.finalizedCopy()
                printDebug("Final: $final")
                nodePostprocessCommon(final)

                // in 3AC, return statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                final.graph.visitAncestors(setOf(final.control) + rvalueNodes(final, stmt.op), nodePostprocess(ReturnLocalNode))
            }
            is ReturnVoidStmt -> {
                val final = input.finalizedCopy()
                nodePostprocessCommon(final)

                final.graph.visitAncestors(setOf(final.control), nodePostprocess(ReturnLocalNode))
            }
            is ThrowStmt -> {
                val final = input.finalizedCopy()
                nodePostprocessCommon(final)

                // in 3AC, throw statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                final.graph.visitAncestors(setOf(final.control) + rvalueNodes(final, stmt.op), nodePostprocess(ThrowLocalNode))

                // TODO handle try-catch
            }
            is DefinitionStmt -> {
                input copyTo output!! // definition stmt must not be last

                val left = stmt.leftOp
                val right = stmt.rightOp

                handleAssign(output, left, right)
            }
            is IfStmt, is SwitchStmt -> {
                val cond = when (stmt) {
                    is SwitchStmt -> stmt.key
                    is IfStmt -> stmt.condition
                    else -> throw AssertionError()
                }
                alwaysAssert(cond !is InvokeExpr) { "if/switch on InvokeExpr condition" }
                val nodes = rvalueNodes(input, cond)
                for (flow in listOf(fallOutList, branchOutList).flatten()) {
                    input copyTo flow
                    for (node in nodes) {
                        flow.graph.touch(node, flow.control) { causes += "Branch" }
                    }
                }
            }
            is NopStmt, is BreakpointStmt, is MonitorStmt -> {
                // nothing to do
                input copyTo output!!
            }
            is PlaceholderStmt -> TODO()
            is InvokeStmt -> {
                input copyTo output!! // InvokeStmt must not be last

                rvalueNodes(output, stmt.invokeExpr)
            }
            is GotoStmt -> {
                alwaysAssert(output === null) { "Goto has no fallout" }
                alwaysAssert(branchOutList.size == 1) { "Goto should branch exactly once" }
                val branch = branchOutList[0]
                input copyTo branch
            }
            else -> throw UnsupportedOperationException("Unsupported operation ${stmt.javaClass}")
        }
        printDebug("Output: $fallOutList")
        printDebug("Branched Output: $branchOutList")
    }

    private fun nodePostprocessCommon(flow: LocalFlow) {
        flow.graph.visitAncestors(setOf(ThisLocalNode), nodePostprocess(ThisLocalNode))
        flow.graph.visitAncestors(setOf(StaticLocalNode), nodePostprocess(StaticLocalNode))
        flow.graph.visitAncestors(setOf(ExplicitSinkLocalNode), nodePostprocess(ExplicitSinkLocalNode))
        for (paramNode in flow.params) {
            println("Searching ancestors of $paramNode")
            flow.graph.visitAncestors(setOf(paramNode)) {
                println("Visited $it")
                nodePostprocess(paramNode)(it)
            }
        }
    }

    private fun nodePostprocess(dest: ContractNode) = { node: LocalNode ->
        if (node is ContractNode && node !== dest) {
            outputContract.graph.addNodeIfMissing(node)
            outputContract.graph.touch(node, dest) {}
        }
    }
}

fun handleAssign(output: LocalFlow, left: Value, right: Value) {
    val leftNodesDelete = lvalueNodes(output, left, LvalueUsage.DELETION)

    for (remove in leftNodesDelete.lvalues) {
        output.graph.deleteAllSources(remove)
    }

    // precompute the nodes to avoid mutations on output from affecting node searches
    val leftNodes = lvalueNodes(output, left, LvalueUsage.ASSIGN)
    val rightNodes = rvalueNodes(output, right)
    val leftRight = rvalueNodes(output, left)
    val rightLeft = lvalueNodes(output, right, LvalueUsage.ASSIGN)

    for (leftNode in leftNodes.lvalues) {
        for (rightNode in rightNodes) {
            output.graph.touch(rightNode, leftNode) { causes += "Assignment" }
        }
        for (rightNode in leftNodes.rvalues) {
            output.graph.touch(rightNode, leftNode) { causes += "Assignment\\nSide effect" }
        }
        output.graph.touch(output.control, leftNode) { causes += "Assignment\\nCondition" }
    }

    for (leftNode in rightLeft.lvalues) {
        for (rightNode in leftRight) {
            output.graph.touch(rightNode, leftNode) { causes += "Assignment\\nBack flow" }
        }
    }
}

fun newLocalFlow(paramCount: Int): LocalFlow {
    val params = List(paramCount) { ParamLocalNode(it) }
    val control = LocalControlNode()
    val graph = makeLocalFlowGraph(params + control)
    return LocalFlow(graph, control, mutableMapOf(), mutableListOf(), params)
}

class LocalFlow(
    val graph: LocalFlowGraph,
    var control: LocalControlNode,
    var locals: MutableMap<String, LocalVarNode>,
    var calls: MutableList<FnCall>,
    var params: List<ParamLocalNode>,
) {
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

    fun finalizedCopy(): LocalFlow {
        val copy = newLocalFlow(params.size)
        this copyTo copy
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

        printDebug("$control copyTo ${dest.control}")
    }

    override fun equals(other: Any?): Boolean {
        if (other !is LocalFlow) return false
        if (graph.filterNodes { it !is LocalControlNode } != other.graph.filterNodes { it !is LocalControlNode }) return false
        return params == other.params && locals == other.locals && calls == other.calls
    }

    override fun hashCode() = throw UnsupportedOperationException("LocalFlow is not hashable")

    override fun toString() = "LocalFlow(control=$control, locals=$locals, params=$params, calls=$calls, graph=$graph)"
}

typealias LocalFlowGraph = MutableDiGraph<LocalNode, LocalEdge>

fun makeLocalFlowGraph(vararg extraNodes: Iterable<LocalNode>): LocalFlowGraph {
    val nodes = indexedSetOf<LocalNode>(ThisLocalNode, StaticLocalNode, ReturnLocalNode, ThrowLocalNode, ExplicitSourceLocalNode, ExplicitSinkLocalNode)
    for (extra in extraNodes) {
        nodes.addAll(extra)
    }

    return newDiGraph(nodes) { LocalEdge(mutableSetOf()) }
}

data class LocalEdge(val causes: MutableSet<String>) : Edge<LocalEdge, LocalNode> {
    /**
     * Indicates that this flow is the back flow of `b -> a`
     * when `copyTo(a, b)` is called.
     */
    var copyFlow: Boolean = false
    var refOnly: Boolean = false

    override fun mergeEdge(other: LocalEdge): LocalEdge {
        val ret = LocalEdge(causes)
        ret.copyFlow = copyFlow && other.copyFlow
        ret.refOnly = refOnly && other.refOnly
        return ret
    }

    override fun graphEqualsImpl(other: Any) = true

    override fun getGraphvizAttributes(from: LocalNode, to: LocalNode): Iterable<Pair<String, String>> {
        return listOf(
            "label" to causes.joinToString(",\\n")
        )
    }
}

data class FnIden(
    val clazz: String,
    val method: String,
)

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
        thisNode.notNull {
            yield(it)
        }
        yield(returnNode)
        yield(throwNode)
    }
}

fun createFnCall(method: SootMethod): FnCall {
    val fn = FnIden(method.declaringClass.name, method.subSignature)
    val params = List(method.parameterCount) { ProxyLocalNode("<$fn>\nparam $it") }
    val thisNode = if (method.isStatic) null else ProxyLocalNode("<$fn>\nthis")
    val returnNode = ProxyLocalNode("<$fn>\nreturn")
    val throwNode = ProxyLocalNode("<$fn>\nthrow")
    val controlNode = ProxyLocalNode("<$fn>\ncontrol")
    return FnCall(fn, params, thisNode, returnNode, throwNode, controlNode)
}
