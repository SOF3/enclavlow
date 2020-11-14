package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.MarkerCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.ExplicitSinkLocalNode
import io.github.sof3.enclavlow.contract.ExplicitSourceLocalNode
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class MarkerTests {
    @Test
    fun returnSource() = testMethod<MarkerCase>("returnSource", makeContract(CallTags.UNSPECIFIED, 0) {
        MethodControlNode into ReturnLocalNode
        ExplicitSourceLocalNode into ReturnLocalNode
    })

    @Test
    fun returnSink() = testMethod<MarkerCase>("returnSink", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        MethodControlNode into ExplicitSinkLocalNode
        ParamLocalNode(0) into ExplicitSinkLocalNode
    })
}
