package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.ThrowNode
import io.github.sof3.enclavlow.cases.lfg.BranchCase
import io.github.sof3.enclavlow.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class BranchTests {
    @Test
    fun conditionalAssign() = testMethod<BranchCase>("conditionalAssign", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ReturnNode
    })

    @Test
    fun conditionalThrow() = testMethod<BranchCase>("conditionalThrow", makeContract(CallTags.UNSPECIFIED, 2) {
        ParamNode(0) into ReturnNode
        ParamNode(0) into ThrowNode
        ParamNode(1) into ThrowNode
    })

    @Test
    fun controlReset() = testMethod<BranchCase>("controlReset", makeContract(CallTags.UNSPECIFIED, 1) {
        // there should be nothing leaked
    })

    @Test
    fun switchMux() = testMethod<BranchCase>("switchMux", makeContract(CallTags.UNSPECIFIED, 5) {
        ParamNode(0) into ReturnNode
        ParamNode(1) into ReturnNode
        ParamNode(2) into ReturnNode
        ParamNode(3) into ReturnNode
        ParamNode(4) into ReturnNode
    })
}
