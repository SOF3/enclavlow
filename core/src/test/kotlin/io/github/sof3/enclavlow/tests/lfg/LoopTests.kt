package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.cases.lfg.LoopCase
import io.github.sof3.enclavlow.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class LoopTests {
    @Test
    fun loopInc() = testMethod<LoopCase>("loopInc", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ReturnNode
    })

    @Test
    fun loopDec() = testMethod<LoopCase>("loopDec", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ReturnNode
    })
}
