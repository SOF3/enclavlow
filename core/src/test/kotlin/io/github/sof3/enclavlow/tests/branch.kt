package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.ThrowNode
import io.github.sof3.enclavlow.cases.BranchLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class BranchLeakTests {
    @Test
    fun conditionalAssign() = testMethod<BranchLeak>("conditionalAssign", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ReturnNode
    })

    @Test
    fun conditionalThrow() = testMethod<BranchLeak>("conditionalThrow", makeContract(CallTags.UNSPECIFIED, 2) {
        ParamNode(0) into ReturnNode
        ParamNode(0) into ThrowNode
        ParamNode(1) into ThrowNode
    })

    @Test
    fun controlReset() = testMethod<BranchLeak>("controlReset", makeContract(CallTags.UNSPECIFIED, 1) {
        // there should be nothing leaked
    })

    @Test
    fun switchMux() = testMethod<BranchLeak>("switchMux", makeContract(CallTags.UNSPECIFIED, 5) {
        ParamNode(0) into ReturnNode
        ParamNode(1) into ReturnNode
        ParamNode(2) into ReturnNode
        ParamNode(3) into ReturnNode
        ParamNode(4) into ReturnNode
    })
}
