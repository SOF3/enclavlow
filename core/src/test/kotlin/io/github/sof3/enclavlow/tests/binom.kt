package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.cases.BinomLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class BinomLeak {
    @Test
    fun binomPlus() = testMethod<BinomLeak>("binomPlus" to makeContract(CallTags.UNSPECIFIED, 2) {
        ParamNode(0) into ReturnNode
        ParamNode(1) into ReturnNode
    })
}
