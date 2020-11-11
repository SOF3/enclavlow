package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.ThrowLocalNode
import io.github.sof3.enclavlow.cases.lfg.BranchCase
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class BranchTests {
    @Test
    fun conditionalAssign() = testMethod<BranchCase>("conditionalAssign", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamLocalNode(0) into ReturnLocalNode
    })

    @Test
    fun conditionalThrow() = testMethod<BranchCase>("conditionalThrow", makeContract(CallTags.UNSPECIFIED, 2) {
        ParamLocalNode(0) into ReturnLocalNode
        ParamLocalNode(0) into ThrowLocalNode
        ParamLocalNode(1) into ThrowLocalNode
    })

    @Test
    fun controlReset() = testMethod<BranchCase>("controlReset", makeContract(CallTags.UNSPECIFIED, 1) {
        // there should be nothing leaked
    })

    @Test
    fun switchMux() = testMethod<BranchCase>("switchMux", makeContract(CallTags.UNSPECIFIED, 5) {
        ParamLocalNode(0) into ReturnLocalNode
        ParamLocalNode(1) into ReturnLocalNode
        ParamLocalNode(2) into ReturnLocalNode
        ParamLocalNode(3) into ReturnLocalNode
        ParamLocalNode(4) into ReturnLocalNode
    })
}
