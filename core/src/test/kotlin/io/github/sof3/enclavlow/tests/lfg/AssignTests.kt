package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.AssignCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.StaticLocalNode
import io.github.sof3.enclavlow.contract.ThisLocalNode
import io.github.sof3.enclavlow.contract.ThrowLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class AssignTests {
    @Test
    fun paramToReturn() = testMethod<AssignCase>("paramToReturn", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        ParamLocalNode(0) into ReturnLocalNode
    })

    @Test
    fun paramToThrow() = testMethod<AssignCase>("paramToThrow", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ThrowLocalNode
        ParamLocalNode(0) into ThrowLocalNode
    })

    @Test
    fun paramToStatic() = testMethod<AssignCase>("paramToStatic", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into StaticLocalNode
        MethodControlNode into ReturnLocalNode
        ParamLocalNode(0) into StaticLocalNode
    })

    @Test
    fun paramToThis() = testMethod<AssignCase>("paramToThis", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        ParamLocalNode(0) into ThisLocalNode
    })

    @Test
    fun thisToStatic() = testMethod<AssignCase>("thisToStatic", makeContract(CallTags.UNSPECIFIED, 0) {
        MethodControlNode into ReturnLocalNode
        ThisLocalNode into StaticLocalNode
    })

    @Test
    fun zeroizeAssign() = testMethod<AssignCase>("zeroizeAssign", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        // there should be nothing leaked
    })

    @Test
    fun paramToParam() = testMethod<AssignCase>("paramToParam", makeContract(CallTags.UNSPECIFIED, 2) {
        MethodControlNode into ReturnLocalNode
        ParamLocalNode(0) into ParamLocalNode(1)
    })

    @Test
    fun assignParam() = testMethod<AssignCase>("assignParam", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        // there should be nothing leaked
    })
}
