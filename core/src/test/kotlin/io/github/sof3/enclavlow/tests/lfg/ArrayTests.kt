package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.ArrayCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class ArrayTests {
    @Test
    fun returnElement() = testMethod<ArrayCase>("returnElement", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        arrayProjection(ParamLocalNode(0)) into ReturnLocalNode
        ParamLocalNode(1) into ReturnLocalNode
    })

    @Test
    fun assignElement() = testMethod<ArrayCase>("assignElement", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        MethodControlNode into arrayProjection(ParamLocalNode(0))
        ParamLocalNode(1) into arrayProjection(ParamLocalNode(0))
        ParamLocalNode(2) into arrayProjection(ParamLocalNode(0))
    })
}
