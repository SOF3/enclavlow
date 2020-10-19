package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamSource
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.cases.BinomLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class BinomLeak {
    @Test
    fun binomPlus() = run<BinomLeak>("binomPlus" to makeContract(2) {
        ParamSource(0) into ReturnScope
        ParamSource(1) into ReturnScope
    })
}
