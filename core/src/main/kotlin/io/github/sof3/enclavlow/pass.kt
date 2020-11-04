package io.github.sof3.enclavlow

import soot.Body
import soot.BodyTransformer
import soot.PackManager
import soot.Transform
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
import soot.tagkit.VisibilityAnnotationTag
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.toolkits.graph.UnitGraph
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis

object SenTransformer : BodyTransformer() {
    val contracts = hashMapOf<Pair<String, String>, Contract<out ContractFlowGraph>>()

    init {
        PackManager.v().getPack("jap").add(Transform("jap.sen", this))
    }

    override fun internalTransform(body: Body, phaseName: String, options: MutableMap<String, String>) {
        var callTags = CallTags.UNSPECIFIED
        for (tag in body.method.tags) {
            if (tag is VisibilityAnnotationTag) {
                for (annot in tag.annotations) {
                    if (annot.type == "Ledu/hku/cs/uranus/IntelSGX;") {
                        callTags = CallTags.ENCLAVE_CALL
                    } else if (annot.type == "Ledu/hku/cs/uranus/IntelSGXOcall;") {
                        callTags = CallTags.OUTSIDE_CALL
                    }
                }
            }
        }

        val flow = SenFlow(ExceptionalUnitGraph(body), body.method.parameterCount, callTags)
        printDebug(body.toString())
        block("Analyzing ${body.method.signature}") {
            flow.doAnalysis()
        }
        contracts[body.method.declaringClass.name to body.method.name] = flow.outputContract
        block("Contract of ${flow.outputContract.callTags} ${body.method.subSignature}") {
            printDebug(flow.outputContract.graph)
        }
    }
}

fun newLocalFlow(paramCount: Int): LocalFlow {
    val params = List(paramCount) { ParamNode(it) }
    val control = ControlNode()
    val graph = makeLocalFlowGraph(params + control)
    return LocalFlow(graph, control, mutableMapOf(), params)
}

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
                    Edge((a.causes + b.causes).toMutableSet())
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

        out.finalizers.addAll(in1.finalizers union in2.finalizers)

        val merge1 = in1.control
        val merge2 = in2.control
        val mergeTarget = out.control
        out.finalizers.addFirst { final ->
            val lca = final.graph.lca(merge1, merge2, { it is ControlNode }, { it.copyFlow })
            if (lca != null) {
                final.graph.touch(lca, mergeTarget, "Flow merge")
            } else {
                final.graph.touch(merge1, mergeTarget, "Flow merge\\nHack")
                final.graph.touch(merge2, mergeTarget, "Flow merge\\nHack")
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
                nodePostprocessCommon(final.graph)

                // in 3AC, return statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                final.graph.visitAncestors(setOf(final.control) + rvalueNodes(final, stmt.op), nodePostprocess(ReturnNode))
            }
            is ReturnVoidStmt -> {
                val final = input.finalizedCopy()
                nodePostprocessCommon(final.graph)

                final.graph.visitAncestors(setOf(final.control), nodePostprocess(ReturnNode))
            }
            is ThrowStmt -> {
                val final = input.finalizedCopy()
                nodePostprocessCommon(final.graph)

                // in 3AC, throw statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                final.graph.visitAncestors(setOf(final.control) + rvalueNodes(final, stmt.op), nodePostprocess(ThrowNode))

                // TODO handle try-catch
            }
            is DefinitionStmt -> {
                input copyTo output!! // definition stmt must not be last

                val left = stmt.leftOp
                val right = stmt.rightOp

                val leftNodesDelete = lvalueNodes(input, left, LvalueUsage.DELETION)

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
                        output.graph.touch(rightNode, leftNode, "Assignment")
                    }
                    for (rightNode in leftNodes.rvalues) {
                        output.graph.touch(rightNode, leftNode, "Assignment\\nSide effect")
                    }
                    output.graph.touch(output.control, leftNode, "Assignment\\nCondition")
                }

                for (leftNode in rightLeft.lvalues) {
                    for (rightNode in leftRight) {
                        output.graph.touch(rightNode, leftNode, "Assignment\\nBack flow")
                    }
                }
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
                        flow.graph.touch(node, flow.control, "Branch")
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

    private fun nodePostprocessCommon(graph: LocalFlowGraph) {
        graph.visitAncestors(setOf(ThisNode), nodePostprocess(ThisNode))
        graph.visitAncestors(setOf(StaticNode), nodePostprocess(StaticNode))
        graph.visitAncestors(setOf(ExplicitSinkNode), nodePostprocess(ExplicitSinkNode))
    }

    private fun nodePostprocess(dest: PublicNode, cause: String = "LFG") = { node: Node ->
        if (node is PublicNode && node !== dest) {
            outputContract.graph.addNodeIfMissing(node)
            outputContract.graph.touch(node, dest, cause)
        }
    }
}

class LocalFlow(
    val graph: LocalFlowGraph,
    var control: ControlNode,
    var locals: MutableMap<String, LocalVarNode>,
    var params: List<ParamNode>,
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
        if (graph.filterNodes { it !is ControlNode } != other.graph.filterNodes { it !is ControlNode }) return false
        return params == other.params && locals == other.locals
    }

    override fun hashCode() = throw UnsupportedOperationException("LocalFlow is not hashable")
}
