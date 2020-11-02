package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.cases.LoopLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class LoopLeakTests {
    @Test
    fun loopAssign() = run<LoopLeak>("loopAssign" to makeContract(1) {
        ParamNode(0) into ReturnScope
    })
}
