package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamSource
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.ThrowScope
import io.github.sof3.enclavlow.cases.BranchLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class ConditionalAssignTests {
    @Test
    fun conditionalAssign() = run<BranchLeak>("conditionalAssign" to makeContract(1) {
        ParamSource(0) into ReturnScope
    })
}

class ConditionalThrowTests {
    @Test
    fun conditionalThrow() = run<BranchLeak>("conditionalThrow" to makeContract(2) {
        ParamSource(0) into ThrowScope
        ParamSource(1) into ThrowScope
    })
}

class ControlResetTests {
    @Test
    fun controlReset() = run<BranchLeak>("controlReset" to makeContract(1) {
        // there should be nothing leaked
    })
}

class LoopAssignTests {
    @Test
    fun loopAssign() = run<BranchLeak>("loopAssign" to makeContract(1) {
        ParamSource(0) into ReturnScope
    })
}
