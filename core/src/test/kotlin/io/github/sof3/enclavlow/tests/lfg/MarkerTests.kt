package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.MarkerCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.ExplicitSinkLocalNode
import io.github.sof3.enclavlow.contract.ExplicitSourceLocalNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class MarkerTests {
    @Test
    fun returnSource() = testMethod<MarkerCase>("returnSource", makeContract(CallTags.UNSPECIFIED, 0) {
        ExplicitSourceLocalNode into ReturnLocalNode
    })

    @Test
    fun returnSink() = testMethod<MarkerCase>("returnSink", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamLocalNode(0) into ExplicitSinkLocalNode
    })
}
