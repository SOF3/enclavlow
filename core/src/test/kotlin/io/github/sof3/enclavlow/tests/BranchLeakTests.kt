package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamSource
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.cases.BranchLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class BranchLeakTests {
    @Test
    fun conditionalAssign() {
        run<BranchLeak>("conditionalAssign" to makeContract(1) {
            ParamSource(0) into ReturnScope
        })
    }
}
