package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.cases.BinomLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class BinomLeak {
    @Test
    fun binomPlus() = run<BinomLeak>("binomPlus" to makeContract(2) {
        ParamNode(0) into ReturnScope
        ParamNode(1) into ReturnScope
    })
}
