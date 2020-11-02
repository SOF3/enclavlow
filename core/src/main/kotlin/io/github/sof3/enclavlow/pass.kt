package io.github.sof3.enclavlow

import org.slf4j.LoggerFactory
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
import soot.toolkits.graph.DirectedGraph
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.toolkits.scalar.ForwardFlowAnalysis

val LOGGER = LoggerFactory.getLogger(SenFlow::class.java)!!

object SenTransformer : BodyTransformer() {
    val contracts = hashMapOf<String, Contract>()

    init {
        PackManager.v().getPack("jtp").add(Transform("jtp.sen", this))
    }

    override fun internalTransform(body: Body, phaseName: String, options: MutableMap<String, String>) {
        val flow = SenFlow(ExceptionalUnitGraph(body), body.method.parameterCount)
        println(body)
        flow.doAnalysis()
        contracts[body.method.name] = flow.outputContract
        println("Contract: ${flow.outputContract}")
    }
}

fun newLocalFlow(paramCount: Int): LocalFlow {
    val params = List(paramCount) { ParamNode(it) }
    val control = ControlNode(null)
    val graph = makeFlowSet(params + control)
    return LocalFlow(graph, mutableMapOf(), params, control)
}

data class LocalFlow(
    val graph: FlowSet,
    var locals: MutableMap<String, VariableNode>,
    var params: List<ParamNode>,
    var control: ControlNode,
) {
    fun pushControl() {
        val newControl = ControlNode(control)
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
    graph: DirectedGraph<soot.Unit>,
    private val paramCount: Int,
) : ForwardFlowAnalysis<soot.Unit, LocalFlow>(graph) {
    val outputContract: MutableContract = makeContract(paramCount)

    override fun newInitialFlow() = newLocalFlow(paramCount)
    override fun merge(in1: LocalFlow, in2: LocalFlow, out: LocalFlow) {
        println("Merging $in1 and $in2")

        in1.graph.merge(in2.graph) { a, b ->
            // TODO handle ControlFlow
            // if flow is detected from either side
            a || b
        }.copyTo(out.graph)

        assert(in1.control.parent == in2.control.parent)
        // pop control
        out.control = in1.control.parent ?: throw AssertionError("Cannot merge flows without parent controls")
    }

    override fun copy(source: LocalFlow, dest: LocalFlow) = source.copyTo(dest)

    public override fun doAnalysis() = super.doAnalysis()

    override fun flowThrough(input: LocalFlow, stmt: soot.Unit, output: LocalFlow) {
        println("${stmt.javaClass.simpleName}: $stmt")
        println("Input: {${input}}")
        input.copyTo(output)

        when (stmt) {
            is ReturnStmt -> {
                input.graph.visitAncestors(setOf(input.control) + rvalueNodes(input, stmt.op), nodePostprocess(ReturnScope))
                input.graph.visitAncestors(setOf(ThisScope), nodePostprocess(ThisScope))
                input.graph.visitAncestors(setOf(StaticScope), nodePostprocess(StaticScope))
            }
            is ReturnVoidStmt -> {
                input.graph.visitAncestors(setOf(input.control), nodePostprocess(ReturnScope))
                input.graph.visitAncestors(setOf(ThisScope), nodePostprocess(ThisScope))
                input.graph.visitAncestors(setOf(StaticScope), nodePostprocess(StaticScope))
            }
            is ThrowStmt -> {
                input.graph.visitAncestors(setOf(input.control) + rvalueNodes(input, stmt.op), nodePostprocess(ThrowScope))
                input.graph.visitAncestors(setOf(ThisScope), nodePostprocess(ThisScope))
                input.graph.visitAncestors(setOf(StaticScope), nodePostprocess(StaticScope))
                // TODO handle try-catch
            }
            is DefinitionStmt -> {
                val left = stmt.leftOp
                val right = stmt.rightOp

                val leftNodesDelete = lvalueNodes(input, left, LvalueUsage.DELETION)

                for (remove in leftNodesDelete.lvalues) {
                    output.graph.deleteAllSources(remove)
                }

                val leftNodes = lvalueNodes(output, left, LvalueUsage.ASSIGN)
                val rightNodes = rvalueNodes(input, right)

                for (leftNode in leftNodes.lvalues) {
                    for (rightNode in rightNodes) {
                        output.graph.touch(rightNode, leftNode)
                    }
                    for (rightNode in leftNodes.rvalues) {
                        output.graph.touch(rightNode, leftNode)
                    }
                    output.graph.touch(output.control, leftNode)
                }

                val leftRight = rvalueNodes(input, left)
                val rightLeft = lvalueNodes(output, right, LvalueUsage.ASSIGN)
                for(leftNode in rightLeft.lvalues) {
                    for(rightNode in leftRight) {
                        output.graph.touch(rightNode, leftNode)
                    }
                }
            }
            is IfStmt, is SwitchStmt -> {
                val cond = if(stmt is SwitchStmt) {
                    stmt.key
                } else if(stmt is IfStmt){
                    stmt.condition
                } else {
                    throw AssertionError()
                }
            }
            is NopStmt, is BreakpointStmt, is MonitorStmt, is GotoStmt -> {
                // no-op
            }
            is PlaceholderStmt -> TODO()
            is InvokeStmt -> {
                // TODO
            }
            else -> throw UnsupportedOperationException("Unsupported operation ${stmt.javaClass}")
        }
        println("Output: {${output}}")
    }

    private fun nodePostprocess(dest: PublicNode) = { node: Node ->
        if (node is PublicNode && node !== dest) {
            outputContract.addNodeIfMissing(node)
            outputContract.touch(node, dest)
        }
    }
}
