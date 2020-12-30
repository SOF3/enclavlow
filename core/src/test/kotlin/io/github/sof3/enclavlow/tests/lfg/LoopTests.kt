package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.LoopCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ProxyLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import java.util.function.BooleanSupplier
import kotlin.test.Test

class LoopTests {
    @Test
    fun loopInc() = testMethod<LoopCase>("loopInc", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        ParamLocalNode(0) into ReturnLocalNode
    })

    @Test
    fun loopDec() = testMethod<LoopCase>("loopDec", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        ParamLocalNode(0) into ReturnLocalNode
    })

    @Test
    fun whileCall() = testMethod<LoopCase>("whileCall", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        MethodControlNode into proxy<BooleanSupplier>("boolean getAsBoolean()", "control")
        MethodControlNode into proxy<BooleanSupplier>("boolean getAsBoolean()", "this")
        ParamLocalNode(0) into proxy<BooleanSupplier>("boolean getAsBoolean()", "this")
    })
}
