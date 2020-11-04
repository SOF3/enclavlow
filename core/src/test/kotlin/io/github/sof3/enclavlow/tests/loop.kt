package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.cases.LoopLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class LoopLeakTests {
    @Test
    fun loopAssign() = testMethod<LoopLeak>("loopAssign", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ReturnNode
    })
}
