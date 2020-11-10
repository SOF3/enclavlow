package io.github.sof3.enclavlow

import java.util.concurrent.atomic.AtomicInteger

/**
 * A node of data flow
 */
sealed class LocalNode

/**
 * A publicly visible node that could appear in the contract
 */
sealed class ContractNode : LocalNode() {
    protected abstract val name: String

    override fun toString() = name
}

/**
 * Data to caller through return path
 */
object ReturnNode : ContractNode() {
    override val name: String
        get() = "return"
}

/**
 * Data to caller through throw path
 */
object ThrowNode : ContractNode() {
    override val name: String
        get() = "throw"
}

/**
 * Data to static scope
 *
 * Data from static scope are always considered insensitive.
 */
object StaticNode : ContractNode() {
    override val name: String
        get() = "static"
}

/**
 * Data from/to `this`
 */
object ThisNode : ContractNode() {
    override val name: String
        get() = "this"
}

/**
 * Data source from a parameter
 */
class ParamNode(private val index: Int) : ContractNode() {
    override val name: String
        get() = "param$index"

    override fun equals(other: Any?) = other is ParamNode && index == other.index

    override fun hashCode() = index.hashCode()
}

/**
 * Data source from a local variable explicitly declared as `sourceMarker`
 */
object ExplicitSourceNode : ContractNode() {
    override val name: String
        get() = "<source>"
}

/**
 * Data source from a local variable explicitly declared as `sinkMarker`
 */
object ExplicitSinkNode : ContractNode() {
    override val name: String
        get() = "<sink>"
}

class ProxyNode(override val name: String) : ContractNode()

/**
 * A private node only considered within method local analysis
 */
sealed class PrivateNode : LocalNode()

/**
 * Data from/to a variable with jimple name `name`
 */
class LocalVarNode(val name: String) : PrivateNode() {
    override fun toString() = name

    override fun equals(other: Any?) = other is LocalVarNode && name == other.name

    override fun hashCode() = name.hashCode()
}

/**
 * Flows to ControlFlow indicates the current control flow contains data
 */
class ControlNode : PrivateNode() {
    private val id = count.getAndAdd(1)

    override fun toString(): String {
        return "control$id"
    }

    companion object {
        private var count = AtomicInteger(0)
    }
}
