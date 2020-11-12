package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.LoopCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class LoopTests {
    @Test
    fun loopInc() = testMethod<LoopCase>("loopInc", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamLocalNode(0) into ReturnLocalNode
    })

    @Test
    fun loopDec() = testMethod<LoopCase>("loopDec", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamLocalNode(0) into ReturnLocalNode
    })
}
