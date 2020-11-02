package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ExplicitSource
import io.github.sof3.enclavlow.ParamNode
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.StaticScope
import io.github.sof3.enclavlow.ThisScope
import io.github.sof3.enclavlow.ThrowScope
import io.github.sof3.enclavlow.cases.DirectLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class DirectLeakTests {
    @Test
    fun paramToReturn() = run<DirectLeak>("paramToReturn" to makeContract(1) {
        ParamNode(0) into ReturnScope
    })

    @Test
    fun paramToThrow() = run<DirectLeak>("paramToThrow" to makeContract(1) {
        ParamNode(0) into ThrowScope
    })

    @Test
    fun paramToStatic() = run<DirectLeak>("paramToStatic" to makeContract(1) {
        ParamNode(0) into StaticScope
    })

    @Test
    fun paramToThis() = run<DirectLeak>("paramToThis" to makeContract(1) {
        ParamNode(0) into ThisScope
    })

    @Test
    fun thisToStatic() = run<DirectLeak>("thisToStatic" to makeContract(0) {
        ThisScope into StaticScope
    })

    @Test
    fun zeroizeAssign() = run<DirectLeak>("zeroizeAssign" to makeContract(1) {
        // there should be nothing leaked
    })

    @Test
    fun returnSource() = run<DirectLeak>("returnSource" to makeContract(0) {
        ExplicitSource into ReturnScope
    })

    @Test
    fun assignParam() = run<DirectLeak>("assignParam" to makeContract(1) {
        // there should be nothing leaked
    })
}
