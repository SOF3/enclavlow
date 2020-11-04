package io.github.sof3.enclavlow

import soot.Body
import soot.BodyTransformer
import soot.PackManager
import soot.Scene
import soot.Transform
import soot.options.Options
import soot.tagkit.VisibilityAnnotationTag
import soot.toolkits.graph.ExceptionalUnitGraph
import java.util.*

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
    return SenTransformer.contracts.get()[className to method.subSignature]!!
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
    val contracts: ThreadLocal<HashMap<Pair<String, String>, Contract<out DiGraph<PublicNode>>>> = ThreadLocal.withInitial { hashMapOf() }!!

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
        printDebug(body.toString())
        block("Analyzing ${body.method.signature}") {
            flow.doAnalysis()
        }
        contracts.get()[body.method.declaringClass.name to body.method.subSignature] = flow.outputContract
        block("Contract of ${flow.outputContract.callTags} ${body.method.subSignature}") {
            printDebug(flow.outputContract.graph)
        }
    }
}
