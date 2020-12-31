package io.github.sof3.enclavlow.contract

import edu.hku.cs.uranus.IntelSGX
import edu.hku.cs.uranus.IntelSGXOcall
import io.github.sof3.enclavlow.local.FnCall
import io.github.sof3.enclavlow.local.FnIden
import io.github.sof3.enclavlow.local.SenFlow
import io.github.sof3.enclavlow.util.DenseGraph
import io.github.sof3.enclavlow.util.Edge
import io.github.sof3.enclavlow.util.MutableDenseGraph
import io.github.sof3.enclavlow.util.block
import io.github.sof3.enclavlow.util.indexedSetOf
import io.github.sof3.enclavlow.util.newDenseGraph
import io.github.sof3.enclavlow.util.printDebug
import soot.Body
import soot.BodyTransformer
import soot.Scene
import soot.SootMethod
import soot.SootResolver
import soot.options.Options
import soot.tagkit.VisibilityAnnotationTag
import soot.toolkits.graph.ExceptionalUnitGraph

fun analyzeMethod(
    className: String,
    methodName: String,
    methodNameType: MethodNameType,
    configure: Options.() -> Unit,
): Contract<out ContractFlowGraph> {
    val body: Body
    val method: SootMethod
    synchronized(soot.Main.v()) {
        soot.G.reset()
        val options = Options.v()
        options.set_output_format(Options.output_format_jimple)
        configure(options)
        SenTransformer.contracts.get().clear()
        val clazz = try {
            Scene.v().loadClassAndSupport(className)
        } catch (e: SootResolver.SootClassNotFoundException) {
            return makeContract(CallTags.UNSPECIFIED, 0) // empty contract
        }
        Scene.v().loadNecessaryClasses()
        clazz.setApplicationClass()
        method = when (methodNameType) {
            MethodNameType.UNIQUE_NAME -> clazz.getMethodByName(methodName)
            MethodNameType.SUB_SIGNATURE -> clazz.getMethod(methodName)
        }
        if (method.isAbstract || method.isNative) {
            // TODO improve: try merging all subclasses
            return degenerateContractFor(method)
        }
        body = method.retrieveActiveBody()
    }
    SenTransformer.transform(body)
    return SenTransformer.contracts.get()[FnIden(method)]!!
}

/**
 * This is a dummy implementation of analyzeMethod for methods that cannot yet be analyzed.
 */
fun degenerateContractFor(method: SootMethod): Contract<out ContractFlowGraph> {
    return makeContract(CallTags.UNSPECIFIED, method.parameterCount) {
        MethodControlNode into ReturnLocalNode

        if (!method.isStatic) {
            ThisLocalNode into ReturnLocalNode
        }

        for (param in 0 until method.parameterCount) {
            ParamLocalNode(param) into ReturnLocalNode
        }
    }
}

enum class MethodNameType {
    /**
     * The method name is unique and has no overloads
     */
    UNIQUE_NAME,

    /**
     * The method name contains parameters and return types.
     *
     * This is obtained from `SootMethod.subSignature`.
     */
    SUB_SIGNATURE,
}

object SenTransformer : BodyTransformer() {
    val contracts: ThreadLocal<HashMap<FnIden, Contract<out ContractFlowGraph>>> = ThreadLocal.withInitial { hashMapOf<FnIden, Contract<out ContractFlowGraph>>() }!!

    override fun internalTransform(body: Body, phaseName: String, options: MutableMap<String, String>) {
        var callTags = CallTags.UNSPECIFIED
        for (tag in body.method.tags) {
            if (tag is VisibilityAnnotationTag) {
                for (annot in tag.annotations) {
                    if (annot.type == "L${IntelSGX::class.java.name.replace('.', '/')};") {
                        callTags = CallTags.ENCLAVE_CALL
                    } else if (annot.type == "L${IntelSGXOcall::class.java.name.replace('.', '/')};") {
                        callTags = CallTags.OUTSIDE_CALL
                    }
                }
            }
        }

        val flow = SenFlow(ExceptionalUnitGraph(body), body.method.parameterCount, FnIden(body.method), callTags)
        printDebug { body.toString() }
        block("Analyzing ${body.method.signature}") {
            flow.doAnalysis()
        }
        contracts.get()[FnIden(body.method)] = flow.outputContract
    }
}

data class Contract<G : ContractFlowGraph>(
    val graph: G,
    val callTags: CallTags,
    val calls: MutableList<FnCall>,
    val methodControl: MethodControlNode,
)

fun makeContract(
    callTags: CallTags,
    paramCount: Int,
    extraNodes: Collection<ContractNode> = emptyList(),
    fn: MakeContractContext<ContractNode, ContractEdge>.() -> Unit = {},
): Contract<MutableContractFlowGraph> {
    val methodControl = MethodControlNode
    val nodes = indexedSetOf(ThisLocalNode, StaticLocalNode, ReturnLocalNode, ThrowLocalNode, ExplicitSourceLocalNode, ExplicitSinkLocalNode)
    nodes.addAll((0 until paramCount).map { ParamLocalNode(it) })
    nodes.addIfMissing(methodControl)
    nodes.addAll(extraNodes)

    val graph = newDenseGraph<ContractNode, ContractEdge>(nodes) {
        ContractEdge(
            refOnly = false,
            projectionBackFlow = false,
        )
    }
    fn(MakeContractContext(graph))
    return Contract(graph, callTags, mutableListOf(), methodControl)
}

typealias ContractFlowGraph = DenseGraph<ContractNode, ContractEdge>
typealias MutableContractFlowGraph = MutableDenseGraph<ContractNode, ContractEdge>

data class ContractEdge(
    var refOnly: Boolean,
    var projectionBackFlow: Boolean,
) : Edge<ContractEdge, ContractNode> {
    override fun mergeEdge(other: ContractEdge) =
        throw UnsupportedOperationException("Contract graphs shall not be merged")

    override fun graphEqualsImpl(other: Any) = true

    override fun getGraphvizAttributes(from: ContractNode, to: ContractNode): Iterable<Pair<String, String>> {
        return listOf(
            "style" to if (refOnly) "dotted" else "solid",
            "color" to if (projectionBackFlow) "blue" else "black",
        )
    }
}

class MakeContractContext<T : Any, E : Edge<E, T>>(private val graph: MutableDenseGraph<T, E>) {
    infix fun T.into(other: T): E? {
        graph.addNodeIfMissing(this)
        graph.addNodeIfMissing(other)
        return graph.touch(this, other) {}
    }

    inline infix fun E?.with(fn: E.() -> Unit): E? {
        this?.fn()
        return this
    }

    fun projection(base: T, name: String): T {
        @Suppress("UNCHECKED_CAST")
        val node = ProjectionNode.create(base as LocalNode, name) as T
        node into base with {
            (this as ContractEdge).projectionBackFlow = true
        }
        return node
    }

    fun arrayProjection(base: T) = projection(base, "<unknown offset>")

    inline fun <reified T> proxy(methodSignature: String, variant: String): ProxyLocalNode {
        val fn = "<${T::class.java.name}.$methodSignature>"
        return ProxyLocalNode("$fn\n$variant")
    }
}

enum class CallTags {
    UNSPECIFIED,
    ENCLAVE_CALL,
    OUTSIDE_CALL,
}
