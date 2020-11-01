package io.github.sof3.enclavlow

import soot.Local
import soot.Value
import soot.jimple.AnyNewExpr
import soot.jimple.ArrayRef
import soot.jimple.BinopExpr
import soot.jimple.CastExpr
import soot.jimple.CaughtExceptionRef
import soot.jimple.Constant
import soot.jimple.InstanceFieldRef
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

fun rvalueNodes(flow: LocalFlow, value: Value): Set<Node> = rvalueNodesImpl(flow, value).toSet()

private fun rvalueNodesImpl(flow: LocalFlow, value: Value): Sequence<Node> = sequence {
    when (value) {
        is Constant -> {
            // constant leaks no information
        }
        is Local -> {
//            val node = flow.locals.getOrFill(value.name) { VariableNode(value.name) }
            val node = flow.locals[value.name]
                ?: throw IllegalArgumentException("rvalue variable should appear as an lvalue first")
            yield(node)
        }
        is ThisRef -> {
            yield(ThisScope)
        }
        is ParameterRef -> {
            yield(flow.params[value.index])
        }
        is CaughtExceptionRef -> TODO()
        is StaticFieldRef -> {
            // static leaks no information
            // TODO: does this leak memory reads?
        }
        is InstanceFieldRef -> {
            // TODO: double check logic
            yieldAll(rvalueNodesImpl(flow, value.base))
        }
        is ArrayRef -> {
            yieldAll(rvalueNodesImpl(flow, value.base))
            yieldAll(rvalueNodesImpl(flow, value.index))
        }
        is UnopExpr -> {
            yieldAll(rvalueNodesImpl(flow, value.op))
        }
        is NewMultiArrayExpr -> {
            for (size in value.sizes) {
                yieldAll(rvalueNodesImpl(flow, size))
            }
        }
        is NewArrayExpr -> {
            yieldAll(rvalueNodesImpl(flow, value.size))
        }
        is NewExpr -> {
            // constructing an object without calling any constructors does not leak anything
        }
        is InvokeExpr -> {
            TODO()
        }
        is CastExpr -> {
            yieldAll(rvalueNodesImpl(flow, value.op))
        }
        is InstanceOfExpr -> {
            yieldAll(rvalueNodesImpl(flow, value.op))
        }
        is BinopExpr -> {
            yieldAll(rvalueNodesImpl(flow, value.op1))
            yieldAll(rvalueNodesImpl(flow, value.op2))
        }

        else -> throw UnsupportedOperationException()
    }
}

data class LvalueResult(
    val lvalues: Set<Node>,
    val rvalues: Set<Node>,
)

fun lvalueNodes(flow: LocalFlow, value: Value, usage: LvalueUsage): LvalueResult {
    val rvalues = mutableSetOf<Node>()
    val lvalues = lvalueNodesImpl(flow, value, usage, rvalues).toSet()
    return LvalueResult(lvalues, rvalues)
}

private fun lvalueNodesImpl(flow: LocalFlow, value: Value, usage: LvalueUsage, rvalues: MutableSet<Node>): Sequence<Node> = sequence {
    println("lvalueNodesImpl(${value.javaClass.simpleName} $value)")
    when (value) {
        // these expressions create new/constant values and can never be mutated in another expression
        is Constant, is InstanceOfExpr, is UnopExpr, is BinopExpr, is AnyNewExpr, is InvokeExpr -> {}
        is ThisRef -> {
            if(usage == LvalueUsage.ASSIGN) {
                yield(ThisScope)
            }
        }
        is ParameterRef -> {
            if(usage == LvalueUsage.ASSIGN) {
                yield(flow.params[value.index])
            }
        }
        is Local -> when (usage) {
            LvalueUsage.ASSIGN -> {
                yield(flow.locals.getOrFill(value.name) {
                    val node = VariableNode(value.name)
                    flow.graph.addNodeIfMissing(node)
                    node
                })
            }
            LvalueUsage.DELETION -> {
                val node = flow.locals[value.name]
                if (node != null) {
                    yield(node)
                }
            }
        }
        is CaughtExceptionRef -> TODO()
        is StaticFieldRef -> {
            yield(StaticScope)
        }
        is InstanceFieldRef -> {
            when (usage) {
                LvalueUsage.ASSIGN -> {
                    yieldAll(lvalueNodesImpl(flow, value.base, usage, rvalues))
                }
                LvalueUsage.DELETION -> {
                    // assigning a.b does not remove sources of a.c
                }
            }
        }
        is ArrayRef -> {
            when (usage) {
                LvalueUsage.ASSIGN -> {
                    yieldAll(lvalueNodesImpl(flow, value.base, usage, rvalues))
                }
                LvalueUsage.DELETION -> {
                    // assigning a[1] does not remove sources of a[0]
                }
            }
            for (rvalue in rvalueNodesImpl(flow, value.index)) {
                rvalues.add(rvalue)
            }
        }
        is CastExpr -> {
            yieldAll(lvalueNodesImpl(flow, value.op, usage, rvalues))
        }
        else -> throw UnsupportedOperationException()
    }
}

enum class LvalueUsage {
    ASSIGN,
    DELETION,
}
