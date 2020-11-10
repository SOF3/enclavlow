package io.github.sof3.enclavlow

import edu.hku.cs.uranus.IntelSGX
import edu.hku.cs.uranus.IntelSGXOcall
import soot.Body
import soot.BodyTransformer
import soot.Scene
import soot.options.Options
import soot.tagkit.VisibilityAnnotationTag
import soot.toolkits.graph.ExceptionalUnitGraph
import java.util.HashMap

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

data class Contract<G : ContractFlowGraph>(val graph: G, val callTags: CallTags, val calls: MutableList<FnCall>)

typealias ContractFlowGraph = DiGraph<ContractNode, ContractEdge>
typealias MutableContractFlowGraph = MutableDiGraph<ContractNode, ContractEdge>

class ContractEdge : GraphEdge<ContractEdge> {
    override fun mergeEdge(other: ContractEdge): ContractEdge {
        TODO("Contract graphs shall not be merged")
    }

    override fun graphEqualsImpl(other: Any) = true

    override fun getGraphvizAttributes(): Iterable<Pair<String, String>> {
        return emptyList() // TODO
    }
}

fun makeContract(
    callTags: CallTags,
    paramCount: Int,
    extraNodes: Collection<ContractNode> = emptyList(),
    fn: MakeContractContext<ContractNode, ContractEdge>.() -> Unit = {},
): Contract<MutableContractFlowGraph> {
    val nodes = indexedSetOf(ThisNode, StaticNode, ReturnNode, ThrowNode, ExplicitSourceNode, ExplicitSinkNode)
    nodes.addAll((0 until paramCount).map { ParamNode(it) })
    nodes.addAll(extraNodes)

    val graph = newDiGraph(nodes) { ContractEdge() }
    fn(MakeContractContext(graph))
    return Contract(graph, callTags, mutableListOf())
}

class MakeContractContext<T : Any, E : GraphEdge<E>>(private val graph: MutableDiGraph<T, E>) {
    infix fun T.into(other: T) {
        graph.touch(this, other)
    }
}

enum class CallTags {
    UNSPECIFIED,
    ENCLAVE_CALL,
    OUTSIDE_CALL,
}
