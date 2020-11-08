package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.CallTags
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.StaticNode
import io.github.sof3.enclavlow.ThisNode
import io.github.sof3.enclavlow.ThrowNode
import io.github.sof3.enclavlow.cases.lfg.AssignCase
import io.github.sof3.enclavlow.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class AssignTests {
    @Test
    fun paramToReturn() = testMethod<AssignCase>("paramToReturn", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ReturnNode
    })

    @Test
    fun paramToThrow() = testMethod<AssignCase>("paramToThrow", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ThrowNode
    })

    @Test
    fun paramToStatic() = testMethod<AssignCase>("paramToStatic", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into StaticNode
    })

    @Test
    fun paramToThis() = testMethod<AssignCase>("paramToThis", makeContract(CallTags.UNSPECIFIED, 1) {
        ParamNode(0) into ThisNode
    })

    @Test
    fun thisToStatic() = testMethod<AssignCase>("thisToStatic", makeContract(CallTags.UNSPECIFIED, 0) {
        ThisNode into StaticNode
    })

    @Test
    fun zeroizeAssign() = testMethod<AssignCase>("zeroizeAssign", makeContract(CallTags.UNSPECIFIED, 1) {
        // there should be nothing leaked
    })

    @Test
    fun paramToParam() = testMethod<AssignCase>("paramToParam", makeContract(CallTags.UNSPECIFIED, 2) {
        ParamNode(0) into ParamNode(1)
    })

    @Test
    fun assignParam() = testMethod<AssignCase>("assignParam", makeContract(CallTags.UNSPECIFIED, 1) {
        // there should be nothing leaked
    })
}
