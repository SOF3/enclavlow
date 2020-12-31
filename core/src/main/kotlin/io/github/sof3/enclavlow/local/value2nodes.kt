package io.github.sof3.enclavlow.local

import io.github.sof3.enclavlow.contract.ExplicitSinkLocalNode
import io.github.sof3.enclavlow.contract.ExplicitSourceLocalNode
import io.github.sof3.enclavlow.contract.LocalNode
import io.github.sof3.enclavlow.contract.StaticLocalNode
import io.github.sof3.enclavlow.contract.ThisLocalNode
import io.github.sof3.enclavlow.util.onlyItem
import soot.Local
import soot.Value
import soot.jimple.AnyNewExpr
import soot.jimple.ArrayRef
import soot.jimple.BinopExpr
import soot.jimple.CastExpr
import soot.jimple.CaughtExceptionRef
import soot.jimple.Constant
import soot.jimple.InstanceFieldRef
import soot.jimple.InstanceInvokeExpr
import soot.jimple.InstanceOfExpr
import soot.jimple.InvokeExpr
import soot.jimple.NewArrayExpr
import soot.jimple.NewExpr
import soot.jimple.NewMultiArrayExpr
import soot.jimple.ParameterRef
import soot.jimple.StaticFieldRef
import soot.jimple.ThisRef
import soot.jimple.UnopExpr

// Conversion between soot.Value and Node

/**
 * Computes the source nodes in flow when value is used as an rvalue
 *
 * `flow` is mutated when `value` is a method invocation.
 */
internal fun rvalueNodes(flow: LocalFlow, value: Value): Set<LocalNode> = rvalueNodesSeq(flow, value).toSet()

private fun rvalueNodesSeq(flow: LocalFlow, value: Value): Sequence<LocalNode> = sequence {
    when (value) {
        is Constant -> {
            // constant leaks no information
        }
        is Local -> {
            val node = flow.getOrAddLocal(value.name) // ?: throw IllegalArgumentException("rvalue variable ${value.name} should appear as an lvalue first")
            yield(node)
        }
        is ThisRef -> {
            yield(ThisLocalNode)
        }
        is ParameterRef -> {
            yield(flow.params[value.index])
        }
        is CaughtExceptionRef -> Unit // TODO: track exception throws
        is StaticFieldRef -> {
            // static leaks no information
            // TODO: does this leak memory reads?
        }
        is InstanceFieldRef -> {
            val node = onlyItem(rvalueNodesSeq(flow, value.base).toList()) { "InstanceFieldRef base has complex nodes" }
            val projection = flow.getProjectionAsNode(node, value.field)
            yield(projection)
        }
        is ArrayRef -> {
//            yieldAll(rvalueNodesSeq(flow, value.base))
            yieldAll(rvalueNodesSeq(flow, value.index))
            val node = onlyItem(rvalueNodesSeq(flow, value.base).toList()) { "ArrayRef base has complex nodes" }
            val projection = flow.getUnknownOffsetProjectionAsNode(node)
            yield(projection)
        }
        is UnopExpr -> {
            yieldAll(rvalueNodesSeq(flow, value.op))
        }
        is NewMultiArrayExpr -> {
            for (size in value.sizes) {
                yieldAll(rvalueNodesSeq(flow, size))
            }
        }
        is NewArrayExpr -> {
            yieldAll(rvalueNodesSeq(flow, value.size))
        }
        is NewExpr -> {
            // constructing an object without invoking the constructor does not leak anything
            // soot will pass another InvokeExpr when the constructor is called
        }
        is InvokeExpr -> {
            if (value.method.declaringClass.name == "io.github.sof3.enclavlow.api.Enclavlow") {
                val method = value.method.name
                if (method == "sourceMarker" || method.endsWith("SourceMarker")) {
                    yield(ExplicitSourceLocalNode)
                } else if (method == "sinkMarker" || method.endsWith("SinkMarker")) {
                    for (arg in value.args) {
                        for (node in rvalueNodes(flow, arg)) {
                            flow.graph.touch(node, ExplicitSinkLocalNode) { causes += LocalFlowCause.SINK_MARKER }
                        }
                    }
                } else {
                    throw IllegalArgumentException("Unknown method in io.github.sof3.enclavlow.api.Enclavlow called")
                }
            } else if (value.method.isNative) {
                // assume all params -> return, no side effects, no exceptions
                for (arg in value.args) {
                    yieldAll(rvalueNodesSeq(flow, arg))
                }
            } else {
                val call = createFnCall(value.method) // TODO specialize for polymorphism
                flow.addCall(call)
                for (node in call.allNodes()) {
                    flow.graph.addNodeIfMissing(node)
                }
                // TODO handle ThrowNode
                for ((i, arg) in value.args.withIndex()) {
                    for (sourceNode in rvalueNodes(flow, arg)) {
                        flow.graph.touch(sourceNode, call.params[i]) { causes += LocalFlowCause.CALL_PARAM }
                    }
                }
                if (value is InstanceInvokeExpr) {
                    for (sourceNode in rvalueNodes(flow, value.base)) {
                        flow.graph.touch(sourceNode, call.thisNode!!) { causes += LocalFlowCause.CALL_CONTEXT }
                    }
                }
                // invokeDynamic and invokeStatic do not pass an objectRef
                flow.graph.touch(flow.control, call.controlNode) { causes += LocalFlowCause.CALL_CONDITION }
                yield(call.returnNode)
            }
        }
        is CastExpr -> {
            yieldAll(rvalueNodesSeq(flow, value.op))
        }
        is InstanceOfExpr -> {
            yieldAll(rvalueNodesSeq(flow, value.op))
        }
        is BinopExpr -> {
            yieldAll(rvalueNodesSeq(flow, value.op1))
            yieldAll(rvalueNodesSeq(flow, value.op2))
        }

        else -> throw UnsupportedOperationException()
    }
}

data class LvalueResult(
    val lvalues: Set<LocalNode>,
    val rvalues: Set<LocalNode>,
)

/**
 * The nodes to be overwritten when a value is written into
 *
 * This returns the *receiver* nodes to be included with the secret.
 */
internal fun lvalueNodes(flow: LocalFlow, value: Value, usage: LvalueUsage): LvalueResult {
    val rvalues = mutableSetOf<LocalNode>()
    val lvalues = lvalueNodesSeq(flow, value, usage, rvalues).toSet()
    return LvalueResult(lvalues, rvalues)
}

private fun lvalueNodesSeq(flow: LocalFlow, value: Value, usage: LvalueUsage, rvalues: MutableSet<LocalNode>): Sequence<LocalNode> = sequence {
    when (value) {
        is Constant, is InstanceOfExpr, is UnopExpr, is BinopExpr, is AnyNewExpr, is InvokeExpr -> {
            // these expressions create new/constant values and can never be mutated in another expression
        }
        is ThisRef -> {
            if (usage == LvalueUsage.ASSIGN) {
                yield(ThisLocalNode)
            }
        }
        is ParameterRef -> {
            if (usage == LvalueUsage.ASSIGN) {
                yield(flow.params[value.index])
            }
        }
        is Local -> when (usage) {
            LvalueUsage.ASSIGN -> {
                yield(flow.getOrAddLocal(value.name))
            }
            LvalueUsage.DELETION -> {
                val node = flow.getLocal(value.name)
                if (node != null) {
                    yield(node)
                }
            }
        }
        is CaughtExceptionRef -> Unit // TODO
        is StaticFieldRef -> {
            yield(StaticLocalNode)
        }
        is InstanceFieldRef -> {
            // Jimple is 3AC, so there is only one rvalue node for base
            val base = onlyItem(rvalueNodesSeq(flow, value.base).toList()) { "InstanceFieldRef base has complex nodes" }

            // // not covered in deletion: even if we overwrite this value again, it will still be passed to outside at some point
            // if (usage == LvalueUsage.ASSIGN) yield(base)

            yield(flow.getProjectionAsNode(base, value.field))
        }
        is ArrayRef -> {
            when (usage) {
                LvalueUsage.ASSIGN -> {
                    val base = onlyItem(rvalueNodesSeq(flow, value.base).toList()) { "ArrayRef base has complex nodes" }
                    yield(flow.getUnknownOffsetProjectionAsNode(base))
                    // yieldAll(lvalueNodesSeq(flow, value.base, usage, rvalues))
                }
                LvalueUsage.DELETION -> {
                    // assigning a[1] does not remove sources of a[0]
                }
            }
            for (rvalue in rvalueNodesSeq(flow, value.index)) {
                rvalues.add(rvalue)
            }
        }
        is CastExpr -> {
            yieldAll(lvalueNodesSeq(flow, value.op, usage, rvalues))
        }
        else -> throw UnsupportedOperationException()
    }
}

enum class LvalueUsage {
    ASSIGN,
    DELETION,
}
