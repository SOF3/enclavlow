package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.BinomCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class BinomTests {
    @Test
    fun binomPlus() = testMethod<BinomCase>("binomPlus", makeContract(CallTags.UNSPECIFIED, 2) {
        MethodControlNode into ReturnLocalNode
        ParamLocalNode(0) into ReturnLocalNode
        ParamLocalNode(1) into ReturnLocalNode
    })
}
