package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.ThrowScope
import io.github.sof3.enclavlow.cases.BranchLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class BranchLeakTests {
    @Test
    fun conditionalAssign() = run<BranchLeak>("conditionalAssign" to makeContract(1) {
        ParamNode(0) into ReturnScope
    })

    @Test
    fun conditionalThrow() = run<BranchLeak>("conditionalThrow" to makeContract(2) {
        ParamNode(0) into ReturnScope
        ParamNode(0) into ThrowScope
        ParamNode(1) into ThrowScope
    })

    @Test
    fun controlReset() = run<BranchLeak>("controlReset" to makeContract(1) {
        // there should be nothing leaked
    })
}
