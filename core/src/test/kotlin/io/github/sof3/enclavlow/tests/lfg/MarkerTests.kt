package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ExplicitSinkNode
import io.github.sof3.enclavlow.ExplicitSourceNode
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.cases.lfg.MarkerCase
import io.github.sof3.enclavlow.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class MarkerTests {
    @Test
    fun returnSource() = testMethod<MarkerCase>("returnSource", makeContract(CallTags.UNSPECIFIED, 0) {
        ExplicitSourceNode into ReturnNode
    })

    @Test
    fun returnSink() = testMethod<MarkerCase>("returnSink", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ExplicitSinkNode
    })
}
