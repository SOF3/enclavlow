package io.github.sof3.enclavlow

import soot.Body
import soot.BodyTransformer
import soot.PackManager
import soot.Transform
import soot.jimple.BreakpointStmt
import soot.jimple.DefinitionStmt
import soot.jimple.GotoStmt
import soot.jimple.IfStmt
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
    val contracts = hashMapOf<String, Contract<out ContractFlowGraph>>()

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
        println()
        println(body)
        flow.doAnalysis()
        contracts[body.method.name] = flow.outputContract
        println("Contract: ${flow.outputContract}")
    }
}

fun newLocalFlow(paramCount: Int): LocalFlow {
    val params = List(paramCount) { ParamNode(it) }
    val control = ControlNode(null, 0)
    val graph = makeLocalFlowGraph(params + control)
    return LocalFlow(graph, mutableMapOf(), params, control)
}

data class LocalFlow(
    val graph: LocalFlowGraph,
    var locals: MutableMap<String, LocalVarNode>,
    var params: List<ParamNode>,
    var control: ControlNode,
) {
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

    fun pushControl(branchId: Int) {
        val newControl = ControlNode(control, branchId)
        graph.addNodeIfMissing(newControl)
        control = newControl
    }

    fun copyTo(dest: LocalFlow) {
        graph.copyTo(dest.graph)
        dest.locals = locals
        dest.params = params
        dest.control = control
    }
}

class SenFlow(
    graph: UnitGraph,
    private val paramCount: Int,
    callTags: CallTags,
) : ForwardBranchedFlowAnalysis<LocalFlow>(graph) {
    val outputContract: Contract<MutableContractFlowGraph> = makeContract(callTags, paramCount)

    override fun newInitialFlow() = newLocalFlow(paramCount)
    override fun merge(in1: LocalFlow, in2: LocalFlow, out: LocalFlow) {
        println("Merging $in1 and $in2")

        in1.graph.merge(in2.graph) { a, b ->
            // TODO handle ControlFlow
            // if flow is detected from either side
            a || b
        }.copyTo(out.graph)

        assert(in1.control.parent == in2.control.parent) { "Merged flows did not diverge from the same parent control" }
        // pop control
        out.locals = mutableMapOf<String, LocalVarNode>().apply {
            putAll(in1.locals)
            putAll(in2.locals)
        }
        out.control = in1.control.parent ?: throw AssertionError("Cannot merge flows without parent controls")
    }

    override fun copy(source: LocalFlow, dest: LocalFlow) = source.copyTo(dest)

    public override fun doAnalysis() = super.doAnalysis()

    override fun flowThrough(input: LocalFlow, stmt: soot.Unit, fallOutList: List<LocalFlow>, branchOutList: List<LocalFlow>) {
        println()
        println("${stmt.javaClass.simpleName}: $stmt")
        println("Input: {$input}")
        val output = fallOutList.getOrNull(0)
        assert(fallOutList.size <= 1) { "Unsupported fallOutList non-singleton" }
        if (output != null) input.copyTo(output)

        when (stmt) {
            is ReturnStmt -> {
                // in 3AC, return statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                input.graph.visitAncestors(setOf(input.control) + rvalueNodes(input, stmt.op), nodePostprocess(ReturnNode))
                nodePostprocessCommon(input.graph)
            }
            is ReturnVoidStmt -> {
                input.graph.visitAncestors(setOf(input.control), nodePostprocess(ReturnNode))
                nodePostprocessCommon(input.graph)
            }
            is ThrowStmt -> {
                // in 3AC, throw statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                input.graph.visitAncestors(setOf(input.control) + rvalueNodes(input, stmt.op), nodePostprocess(ThrowNode))
                nodePostprocessCommon(input.graph)
                // TODO handle try-catch
            }
            is DefinitionStmt -> {
                output!! // definition stmt must not be last

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
                        output.graph.touch(rightNode, leftNode)
                    }
                    for (rightNode in leftNodes.rvalues) {
                        output.graph.touch(rightNode, leftNode)
                    }
                    output.graph.touch(output.control, leftNode)
                }

                for (leftNode in rightLeft.lvalues) {
                    for (rightNode in leftRight) {
                        output.graph.touch(rightNode, leftNode)
                    }
                }
            }
            is IfStmt, is SwitchStmt -> {
                output!! // conditional stmt must not be last

                val cond = when (stmt) {
                    is SwitchStmt -> stmt.key
                    is IfStmt -> stmt.condition
                    else -> throw AssertionError()
                }
                val nodes = rvalueNodes(output, cond)
                for (branchedOutput in branchOutList) {
                    input.copyTo(branchedOutput)
                    for ((i, flow) in setOf(output, branchedOutput).withIndex()) {
                        flow.pushControl(i)
                        for (node in nodes) {
                            flow.graph.touch(node, flow.control)
                        }
                    }
                }
            }
            is NopStmt, is BreakpointStmt, is MonitorStmt, is GotoStmt -> {
                // no-op
            }
            is PlaceholderStmt -> TODO()
            is InvokeStmt -> {
                output!! // InvokeStmt must not be last

                rvalueNodes(output, stmt.invokeExpr)
            }
            else -> throw UnsupportedOperationException("Unsupported operation ${stmt.javaClass}")
        }
        println("Output: $fallOutList")
        println("Branched Output: $branchOutList")
    }

    private fun nodePostprocessCommon(graph: LocalFlowGraph) {
        graph.visitAncestors(setOf(ThisNode), nodePostprocess(ThisNode))
        graph.visitAncestors(setOf(StaticNode), nodePostprocess(StaticNode))
        graph.visitAncestors(setOf(ExplicitSinkNode), nodePostprocess(ExplicitSinkNode))
    }

    private fun nodePostprocess(dest: PublicNode) = { node: Node ->
        if (node is PublicNode && node !== dest) {
            outputContract.graph.addNodeIfMissing(node)
            outputContract.graph.touch(node, dest)
        }
    }
}
