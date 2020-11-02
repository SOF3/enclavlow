package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnNode
import io.github.sof3.enclavlow.StaticNode
import io.github.sof3.enclavlow.ThisNode
import io.github.sof3.enclavlow.ThrowNode
import io.github.sof3.enclavlow.cases.AssignLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class AssignLeakTests {
    @Test
    fun paramToReturn() = testMethod<AssignLeak>("paramToReturn" to makeContract(1) {
        ParamNode(0) into ReturnNode
    })

    @Test
    fun paramToThrow() = testMethod<AssignLeak>("paramToThrow" to makeContract(1) {
        ParamNode(0) into ThrowNode
    })

    @Test
    fun paramToStatic() = testMethod<AssignLeak>("paramToStatic" to makeContract(1) {
        ParamNode(0) into StaticNode
    })

    @Test
    fun paramToThis() = testMethod<AssignLeak>("paramToThis" to makeContract(1) {
        ParamNode(0) into ThisNode
    })

    @Test
    fun thisToStatic() = testMethod<AssignLeak>("thisToStatic" to makeContract(0) {
        ThisNode into StaticNode
    })

    @Test
    fun zeroizeAssign() = testMethod<AssignLeak>("zeroizeAssign" to makeContract(1) {
        // there should be nothing leaked
    })

    @Test
    fun assignParam() = testMethod<AssignLeak>("assignParam" to makeContract(1) {
        // there should be nothing leaked
    })
}
