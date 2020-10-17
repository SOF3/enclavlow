package io.github.sof3.enclavlow

import soot.toolkits.scalar.ArraySparseSet

typealias SenFlowSet = ArraySparseSet<Flow>

sealed class Flow {
    abstract val sources: Set<Source>
}

class VariableFlow(val name: String, override val sources: Set<Source>) : Flow()

class ExitFlow(override val sources: Set<Source>, val exitType: ExitType) : Flow()

enum class ExitType {
    RETURN,
    THROW,
}
