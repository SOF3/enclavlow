package io.github.sof3.enclavlow.local

import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.Contract
import io.github.sof3.enclavlow.contract.ContractNode
import io.github.sof3.enclavlow.contract.ControlNode
import io.github.sof3.enclavlow.contract.ExplicitSinkLocalNode
import io.github.sof3.enclavlow.contract.LocalControlNode
import io.github.sof3.enclavlow.contract.LocalNode
import io.github.sof3.enclavlow.contract.LocalVarNode
import io.github.sof3.enclavlow.contract.MutableContractFlowGraph
import io.github.sof3.enclavlow.contract.ProjectionNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.StaticLocalNode
import io.github.sof3.enclavlow.contract.ThisLocalNode
import io.github.sof3.enclavlow.contract.ThrowLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.util.alwaysAssert
import io.github.sof3.enclavlow.util.block
import io.github.sof3.enclavlow.util.printDebug
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
import java.io.File

var PRINT_DOT = false
val DOT_FILENAME_REGEX = Regex("[^A-Za-z0-9]+")

class SenFlow(
    graph: UnitGraph,
    private val paramCount: Int,
    private val fn: FnIden,
    callTags: CallTags,
) : ForwardBranchedFlowAnalysis<LocalFlow>(graph) {
    val outputContract: Contract<MutableContractFlowGraph> = makeContract(callTags, paramCount)
    private var dotWrites: Int = 0

    override fun newInitialFlow() = newLocalFlow(paramCount, LocalControlNode(), this)
    override fun merge(in1: LocalFlow, in2: LocalFlow, out: LocalFlow) = block("Merge") {
        in1.graph.merge(in2.graph) { a, b ->
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
        out.projections = mutableSetOf<ProjectionNode<*>>().apply {
            addAll(in1.projections)
            addAll(in2.projections)
        }

        out.finalizers.addAll(in1.finalizers union in2.finalizers)

        val merge1 = in1.control
        val merge2 = in2.control
        val mergeTarget = out.control
        out.finalizers.addFirst { final ->
            val lca = final.graph.lca(merge1, merge2, { it is ControlNode }, { false }) // TODO check, copyFlow always false?
            if (lca != null) {
                final.graph.touch(lca, mergeTarget) { causes += LocalFlowCause.FLOW_MERGE }
            } else {
                final.graph.touch(merge1, mergeTarget) { causes += LocalFlowCause.FLOW_MERGE_HACK }
                final.graph.touch(merge2, mergeTarget) { causes += LocalFlowCause.FLOW_MERGE_HACK }
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
        printDebug { "${stmt.javaClass.simpleName}: $stmt" }
        // printDebug { "Input: $input" }
        val output = fallOutList.getOrNull(0)
        if (fallOutList.size > 1) throw AssertionError("Unsupported fallOutList non-singleton")

        when (stmt) {
            is ReturnStmt -> {
                val final = input.finalizedCopy(outputContract.methodControl)
                if (PRINT_DOT) writeDotFile(final)

                ancestorPostprocessCommon(final)

                // in 3AC, return statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                ancestorPostprocess(final, setOf(final.control) + rvalueNodes(final, stmt.op), ReturnLocalNode)
            }
            is ReturnVoidStmt -> {
                val final = input.finalizedCopy(outputContract.methodControl)
                if (PRINT_DOT) writeDotFile(final)

                ancestorPostprocessCommon(final)

                ancestorPostprocess(final, setOf(final.control), ReturnLocalNode)
            }
            is ThrowStmt -> {
                val final = input.finalizedCopy(outputContract.methodControl)
                if (PRINT_DOT) writeDotFile(final)

                ancestorPostprocessCommon(final)

                // in 3AC, throw statements are never followed by invocation calls
                // so it is safe to just pass input to rvalueNodes
                ancestorPostprocess(final, setOf(final.control) + rvalueNodes(final, stmt.op), ThrowLocalNode)

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
                        flow.graph.touch(node, flow.control) { causes += LocalFlowCause.BRANCH }
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
            else -> throw UnsupportedOperationException("Unsupported statement ${stmt.javaClass} $stmt")
        }
        printDebug { "Output: $fallOutList" }
        printDebug { "Branched Output: $branchOutList" }
    }

    private fun ancestorPostprocessCommon(flow: LocalFlow) {
        ancestorPostprocess(flow, setOf(ThisLocalNode), ThisLocalNode)
        ancestorPostprocess(flow, setOf(StaticLocalNode), StaticLocalNode)
        ancestorPostprocess(flow, setOf(ExplicitSinkLocalNode), ExplicitSinkLocalNode)
        for (paramNode in flow.params) {
            ancestorPostprocess(flow, setOf(paramNode), paramNode)
        }
        for (call in flow.calls) {
            for (node in call.allNodes()) {
                if (node in flow.graph.nodes) ancestorPostprocess(flow, setOf(node), node)
            }
        }
    }

    /**
     * Mark each node in `leaves` as a `dest` node
     * and search the corresponding flow.
     */
    private fun ancestorPostprocess(flow: LocalFlow, leaves: Set<LocalNode>, dest: ContractNode) {
        flow.graph.visitAncestors(leaves, { e1, e2 ->
            // if e1 is a cut-edge but not all edges have refOnly, discard it
            if (e1.causes.all { it.projectionBackFlow } != e2.causes.all { it.projectionBackFlow }) return@visitAncestors null
            LocalEdge((e1.causes + e2.causes).toMutableSet())
        }, { edge: LocalEdge, node: LocalNode ->
            if (node is ContractNode && node !== dest) {
                outputContract.graph.addNodeIfMissing(node)
                outputContract.graph.addNodeIfMissing(dest)
                outputContract.graph.touch(node, dest) {
                    projectionBackFlow = edge.causes.all { it.projectionBackFlow }
                }
            }
        })

        for (projection in flow.projections) {
            if (projection.base == dest && projection is ContractNode) {
                ancestorPostprocess(flow, setOf(projection), projection)
            }
        }
    }


    private fun writeDotFile(flow: LocalFlow) = block("Printing result LFG to dot") {
        val dir = File("build/lfgOutput")
        dir.mkdirs()
        val fileName = fn.toString().replace(DOT_FILENAME_REGEX, ".")
            .removeSuffix(".") + "_${dotWrites++}"
        val dot = File(dir, "$fileName.dot")
        dot.writeText(flow.graph.toGraphviz(fileName.replace(".", "_")))
        val command = listOf("dot", "-T", "svg", "-o", "${dot.path}.svg", dot.path)
        printDebug { "Rendering graph: ${command.joinToString(" ")}" }
        ProcessBuilder(command)
            .redirectOutput(File("/dev/stderr"))
            .start()
            .waitFor()

        synchronized(javaClass) { // prevent race conditions writing index.html
            File(dir, "index.html").appendText("<li><a href=\"$fileName.dot.svg\">$fn (${dotWrites - 1})</a></li>\n")
        }
    }
}

private fun handleAssign(output: LocalFlow, left: Value, right: Value) {
    // TODO improve mechanism with loops
    // val leftNodesDelete = lvalueNodes(output, left, LvalueUsage.DELETION)

    // for (remove in leftNodesDelete.lvalues) {
    // printDebug { "NOT Deleting all edges into $remove due to overwrite" }
    // }

    // precompute the nodes to avoid mutations on output from affecting node searches
    val leftNodes = lvalueNodes(output, left, LvalueUsage.ASSIGN)
    val rightNodes = rvalueNodes(output, right)

    for (leftNode in leftNodes.lvalues) {
        for (rightNode in rightNodes) {
            output.graph.touch(rightNode, leftNode) { causes += LocalFlowCause.ASSIGNMENT }
        }

        // side effects like array index
        for (rightNode in leftNodes.rvalues) {
            output.graph.touch(rightNode, leftNode) { causes += LocalFlowCause.ASSIGNMENT_SIDE_EFFECT }
        }
        output.graph.touch(output.control, leftNode) { causes += LocalFlowCause.ASSIGNMENT_CONDITION }
    }

    // for (leftNode in rightLeft.lvalues) {
    // for (rightNode in leftRight) {
    // output.graph.touch(rightNode, leftNode) { causes += LocalFlowCause.REF_BACK_FLOW; }
    // }
    // }
}
