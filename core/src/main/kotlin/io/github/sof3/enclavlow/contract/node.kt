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
 * Data to static scope
 *
 * Data from static scope are always considered insensitive.
 */
object StaticLocalNode : ContractNode() {
    override val name: String
        get() = "static"
}

sealed class ScopedContractNode : ContractNode() {
    abstract fun toOwned(): ScopedContractNode
}

/**
 * Data to caller through return path
 */
object ReturnLocalNode : ScopedContractNode() {
    override val name: String
        get() = "return"

    override fun toOwned() = this
}

/**
 * Data to caller through throw path
 */
object ThrowLocalNode : ScopedContractNode() {
    override val name: String
        get() = "throw"

    override fun toOwned() = this
}

/**
 * Data from/to `this`
 */
object ThisLocalNode : ScopedContractNode() {
    override val name: String
        get() = "this"

    override fun toOwned() = this
}

/**
 * Data source from a parameter
 */
data class ParamLocalNode(private val index: Int) : ScopedContractNode() {
    override val name: String
        get() = "param$index"

    override fun equals(other: Any?) = other is ParamLocalNode && index == other.index

    override fun hashCode() = index.hashCode()

    override fun toOwned() = copy()
}

object MethodControlNode : ScopedContractNode() {
    override val name: String
        get() = "methodCall"

    override fun toOwned() = this
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
