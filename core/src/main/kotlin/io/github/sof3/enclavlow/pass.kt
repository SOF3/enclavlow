package io.github.sof3.enclavlow

import org.slf4j.LoggerFactory
import soot.Body
import soot.BodyTransformer
import soot.Local
import soot.Value
import soot.jimple.DefinitionStmt
import soot.jimple.InstanceFieldRef
import soot.jimple.ParameterRef
import soot.jimple.ReturnStmt
import soot.jimple.ReturnVoidStmt
import soot.jimple.ThisRef
import soot.jimple.ThrowStmt
import soot.toolkits.graph.DirectedGraph
import soot.toolkits.graph.ExceptionalUnitGraph
import soot.toolkits.scalar.ForwardFlowAnalysis
import java.io.File

private val LOGGER = LoggerFactory.getLogger(SenFlow::class.java)

object SenTransformer : BodyTransformer() {
    override fun internalTransform(body: Body, phaseName: String, options: MutableMap<String, String>) {
        val flow = SenFlow(ExceptionalUnitGraph(body))
        flow.doAnalysis()
    }
}

class SenFlow(graph: DirectedGraph<soot.Unit>) : ForwardFlowAnalysis<soot.Unit, SenFlowSet>(graph) {
    override fun newInitialFlow() = SenFlowSet()
    override fun merge(in1: SenFlowSet, in2: SenFlowSet, out: SenFlowSet) = in1.union(in2, out)
    override fun copy(source: SenFlowSet, dest: SenFlowSet) = source.copy(dest)

    public override fun doAnalysis() = super.doAnalysis()

    override fun flowThrough(input: SenFlowSet, node: soot.Unit, output: SenFlowSet) {
        when (node) {
            is ReturnStmt -> {
                val sources = findSources(input, node.op)
                output.add(ExitFlow(sources, ExitType.RETURN))
            }
            is ReturnVoidStmt -> {
                /* no-op */
            }
            is ThrowStmt -> {
                val sources = findSources(input, node.op)
                output.add(ExitFlow(sources, ExitType.THROW))
            }
            is DefinitionStmt -> {
                val left = node.leftOp
                val right = node.rightOp

                input.copy(output)
                removeValue(output, left)

                val sources = findSources(input, right)
                if (sources.isNotEmpty()) {
                    output.add(createFlowFromValue(left, sources))
                }
            }
            else -> {
                LOGGER.warn("Unhandled passthru node ${node.javaClass}: $node")
                input.copy(output)
            }
        }
    }
}

private fun findSources(flowSet: SenFlowSet, value: Value): Set<Source> = when (value) {
    is ParameterRef -> setOf(ParamSource(value.index))
    is ThisRef -> setOf(ThisSource)
    is Local -> {
        flowSet.firstOrNull { it is VariableFlow && it.name == value.name }?.sources ?: emptySet()
    }
    else -> {
        LOGGER.warn("Unhandled value ${value.javaClass}: $value")
        emptySet()
    }
}

private fun removeValue(flowSet: SenFlowSet, value: Value) {
    // remove
}

private fun createFlowFromValue(value: Value, sources: Set<Source>) = when (value) {
    is Local -> VariableFlow(value.name, sources)
    is InstanceFieldRef -> {
        // case 1: secret.i = 1
        // case 2: leak.i = 1
        // case 3: secret.leak.i = 1
        // case 4: leak.secret.i = 1
        TODO("not yet implemented")
    }
    else -> TODO("not yet implemented")
}
