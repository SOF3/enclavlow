package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.cases.lfg.BinomCase
import io.github.sof3.enclavlow.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class BinomTests {
    @Test
    fun binomPlus() = testMethod<BinomCase>("binomPlus", makeContract(CallTags.UNSPECIFIED, 2) {
        ParamNode(0) into ReturnNode
        ParamNode(1) into ReturnNode
    })
}
