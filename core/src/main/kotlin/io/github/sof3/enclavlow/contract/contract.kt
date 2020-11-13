package io.github.sof3.enclavlow.contract

import edu.hku.cs.uranus.IntelSGX
import edu.hku.cs.uranus.IntelSGXOcall
import io.github.sof3.enclavlow.local.FnCall
import io.github.sof3.enclavlow.local.FnIden
import io.github.sof3.enclavlow.local.SenFlow
import io.github.sof3.enclavlow.util.DiGraph
import io.github.sof3.enclavlow.util.Edge
import io.github.sof3.enclavlow.util.MutableDiGraph
import io.github.sof3.enclavlow.util.block
import io.github.sof3.enclavlow.util.indexedSetOf
import io.github.sof3.enclavlow.util.newDiGraph
import io.github.sof3.enclavlow.util.printDebug
import soot.Body
import soot.BodyTransformer
import soot.Scene
import soot.options.Options
import soot.tagkit.VisibilityAnnotationTag
import soot.toolkits.graph.ExceptionalUnitGraph

inline fun analyzeMethod(
    className: String,
    methodName: String,
    methodNameType: MethodNameType,
    configure: Options.() -> Unit,
): Contract<out ContractFlowGraph> {
    soot.G.reset()
    val options = Options.v()
    options.set_output_format(Options.output_format_jimple)
    configure(options)
    SenTransformer.contracts.get().clear()
    val clazz = Scene.v().loadClassAndSupport(className)
    Scene.v().loadNecessaryClasses()
    clazz.setApplicationClass()
    val method = when (methodNameType) {
        MethodNameType.UNIQUE_NAME -> clazz.getMethodByName(methodName)
        MethodNameType.SUB_SIGNATURE -> clazz.getMethod(methodName)
    }
    val body = method.retrieveActiveBody()
    SenTransformer.transform(body)
    return SenTransformer.contracts.get()[FnIden(className, method.subSignature)]!!
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

        val flow = SenFlow(ExceptionalUnitGraph(body), body.method.parameterCount, callTags)
        printDebug(body.toString())
        block("Analyzing ${body.method.signature}") {
            flow.doAnalysis()
        }
        contracts.get()[FnIden(body.method.declaringClass.name, body.method.subSignature)] = flow.outputContract
        block("Contract of ${flow.outputContract.callTags} ${body.method.subSignature}") {
            printDebug(flow.outputContract.graph)
        }
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

    val graph = newDiGraph(nodes) {
        ContractEdge(
            refOnly = false,
            projectionBackflow = false,
        )
    }
    fn(MakeContractContext(graph))
    return Contract(graph, callTags, mutableListOf(), methodControl)
}

typealias ContractFlowGraph = DiGraph<ContractNode, ContractEdge>
typealias MutableContractFlowGraph = MutableDiGraph<ContractNode, ContractEdge>

data class ContractEdge(
    var refOnly: Boolean,
    var projectionBackflow: Boolean,
) : Edge<ContractEdge, ContractNode> {
    override fun mergeEdge(other: ContractEdge) =
        throw UnsupportedOperationException("Contract graphs shall not be merged")

    override fun graphEqualsImpl(other: Any) = true

    override fun getGraphvizAttributes(from: ContractNode, to: ContractNode): Iterable<Pair<String, String>> {
        return listOf(
            "color" to when {
                refOnly -> "grey"
                projectionBackflow -> "blue"
                else -> "black"
            },
        )
    }
}

class MakeContractContext<T : Any, E : Edge<E, T>>(private val graph: MutableDiGraph<T, E>) {
    infix fun T.into(other: T): E? {
        return graph.touch(this, other) {}
    }

    inline infix fun E?.with(fn: E.() -> Unit): E? {
        this?.fn()
        return this
    }
}

enum class CallTags {
    UNSPECIFIED,
    ENCLAVE_CALL,
    OUTSIDE_CALL,
}
