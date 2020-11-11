package io.github.sof3.enclavlow.contract

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
object ReturnLocalNode : ContractNode() {
    override val name: String
        get() = "return"
}

/**
 * Data to caller through throw path
 */
object ThrowLocalNode : ContractNode() {
    override val name: String
        get() = "throw"
}

/**
 * Data to static scope
 *
 * Data from static scope are always considered insensitive.
 */
object StaticLocalNode : ContractNode() {
    override val name: String
        get() = "static"
}

/**
 * Data from/to `this`
 */
object ThisLocalNode : ContractNode() {
    override val name: String
        get() = "this"
}

/**
 * Data source from a parameter
 */
class ParamLocalNode(private val index: Int) : ContractNode() {
    override val name: String
        get() = "param$index"

    override fun equals(other: Any?) = other is ParamLocalNode && index == other.index

    override fun hashCode() = index.hashCode()
}

/**
 * Data source from a local variable explicitly declared as `sourceMarker`
 */
object ExplicitSourceLocalNode : ContractNode() {
    override val name: String
        get() = "<source>"
}

/**
 * Data source from a local variable explicitly declared as `sinkMarker`
 */
object ExplicitSinkLocalNode : ContractNode() {
    override val name: String
        get() = "<sink>"
}

class ProxyLocalNode(override val name: String) : ContractNode()

/**
 * A private node only considered within method local analysis
 */
sealed class LocalOnlyNode : LocalNode()

/**
 * Data from/to a variable with jimple name `name`
 */
class LocalVarNode(val name: String) : LocalOnlyNode() {
    override fun toString() = name

    override fun equals(other: Any?) = other is LocalVarNode && name == other.name

    override fun hashCode() = name.hashCode()
}

/**
 * Flows to ControlFlow indicates the current control flow contains data
 */
class LocalControlNode : LocalOnlyNode() {
    private val id = count.getAndAdd(1)

    override fun toString(): String {
        return "control$id"
    }

    companion object {
        private var count = AtomicInteger(0)
    }
}
