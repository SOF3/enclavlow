package io.github.sof3.enclavlow

import org.slf4j.LoggerFactory
import soot.Body
import soot.BodyTransformer
import soot.Local
import soot.PackManager
import soot.Transform
import soot.Value
import soot.jimple.BinopExpr
import soot.jimple.DefinitionStmt
import soot.jimple.IfStmt
import soot.jimple.InstanceFieldRef
import soot.jimple.ParameterRef
import soot.jimple.ReturnStmt
import soot.jimple.ReturnVoidStmt
import soot.jimple.StaticFieldRef
import soot.jimple.ThisRef
import soot.jimple.ThrowStmt
import soot.toolkits.graph.DirectedGraph
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.toolkits.scalar.ForwardFlowAnalysis

private val LOGGER = LoggerFactory.getLogger(SenFlow::class.java)

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

class SenFlow(graph: DirectedGraph<soot.Unit>, private val paramCount: Int) : ForwardFlowAnalysis<soot.Unit, FlowSet>(graph) {
    val outputContract: MutableContract = makeContract(paramCount)

    override fun newInitialFlow() = makeFlowSet(paramCount)
    override fun merge(in1: FlowSet, in2: FlowSet, out: FlowSet) {
        println("Merging $in1 and $in2")
        in1.merge(in2) { a, b ->
            // TODO handle ControlFlow
            // if flow is detected from either side
            a || b
        }.copyTo(out)
    }

    override fun copy(source: FlowSet, dest: FlowSet) = source.copyTo(dest)

    public override fun doAnalysis() = super.doAnalysis()

    override fun flowThrough(input: FlowSet, node: soot.Unit, output: FlowSet) {
        println("${node.javaClass.simpleName}: $node")
        println("Input: {$input}")
        when (node) {
            is ReturnStmt -> {
                outputContract.touchSources(findSourcesForValue(input, node.op), ReturnScope)
                outputContract.touchSources(input.findSources(ControlFlow, canBeSelf = false).subtype()!!, ReturnScope)
                writeGlobalOutput(input)
            }
            is ReturnVoidStmt -> {
                writeGlobalOutput(input)
                outputContract.touchSources(input.findSources(ControlFlow, canBeSelf = false).subtype()!!, ReturnScope)
            }
            is ThrowStmt -> {
                outputContract.touchSources(findSourcesForValue(input, node.op), ThrowScope)
                outputContract.touchSources(input.findSources(ControlFlow, canBeSelf = false).subtype()!!, ThrowScope)
                writeGlobalOutput(input)
            }
            is DefinitionStmt -> {
                val left = node.leftOp
                val right = node.rightOp

                input.copyTo(output)
                removeValue(output, left)

                val sources = findSourcesForValue(input, right) + input.findSources(ControlFlow, canBeSelf = false).subtype()!!
                if (sources.isNotEmpty()) {
                    addNodesToValue(output, left, sources)
                }
            }
            is IfStmt -> {
                input.copyTo(output)
                output.touchSources(findSourcesForValue(input, node.condition), ControlFlow)
            }
            else -> {
                LOGGER.warn("Unhandled passthru node ${node.javaClass}: $node")
                input.copyTo(output)
            }
        }
        println("Output: {$output}")
    }

    private fun writeGlobalOutput(flow: FlowSet) {
        println("Writing global output")
        for (source in flow.findSources(ThisScope, canBeSelf = false)) {
            println("Append outputContract: $source -> <this>")
            outputContract.touch(source as PublicNode, ThisScope)
        }
        for (source in flow.findSources(StaticScope, canBeSelf = false)) {
            println("Append outputContract: $source -> <static>")
            outputContract.touch(source as PublicNode, StaticScope)
        }
    }
}

/**
 * Returns the set of ultimate sources affecting a value
 */
private fun findSourcesForValue(flowSet: FlowSet, value: Value): Set<PublicNode> = when (value) {
    is ParameterRef -> setOf(ParamSource(value.index))
    is ThisRef -> setOf(ThisScope)
    is Local -> {
        val variable = flowSet.nodes.firstOrNull { it is VariableNode && it.name == value.name }
        if (variable != null) {
            flowSet.findSources(variable).subtype() ?: throw ClassCastException("Non-PublicNode data sources found")
        } else {
            emptySet()
        }
    }
    is InstanceFieldRef -> findSourcesForValue(flowSet, value.base)
    is BinopExpr -> findSourcesForValue(flowSet, value.op1) + findSourcesForValue(flowSet, value.op2)
    else -> {
        LOGGER.warn("Unhandled value ${value.javaClass}: $value")
        emptySet()
    }
}

private fun removeValue(flowSet: FlowSet, value: Value) {
    // TODO remove
}

private fun addNodesToValue(flowSet: FlowSet, dest: Value, sources: Set<PublicNode>): Unit = when (dest) {
    // TODO verify different logic for lvalue vs rvalue

    is Local -> {
        val variable = VariableNode(dest.name)
        println(variable)
        flowSet.addNodeIfMissing(variable)
        for (source in sources) {
            println("$source -> $variable")
            flowSet.touch(source, variable)
        }
    }
    is InstanceFieldRef -> {
        // even public.secret.c = 1 is still a leak
        // therefore, instance assignment propagate secret requirement to all sources
        // dest.base is a reference, so we need to propagate further
        val leftSources = findSourcesForValue(flowSet, dest.base)
        for (left in leftSources) {
            for (source in sources) {
                flowSet.touch(source, left)
            }
        }
    }
    is StaticFieldRef -> {
        for (source in sources) {
            flowSet.touch(source, StaticScope)
        }
    }
    else -> TODO("not yet implemented: ${dest.javaClass.simpleName}")
}
