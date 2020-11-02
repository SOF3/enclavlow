package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ExplicitSinkNode
import io.github.sof3.enclavlow.ExplicitSourceNode
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.cases.MarkerLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class MarkerLeakTests {
    @Test
    fun returnSource() = testMethod<MarkerLeak>("returnSource" to makeContract(CallTags.UNSPECIFIED, 0) {
        ExplicitSourceNode into ReturnNode
    })

    @Test
    fun returnSink() = testMethod<MarkerLeak>("returnSource" to makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ExplicitSinkNode
    })
}
