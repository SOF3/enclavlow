package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.ParamSource
import io.github.sof3.enclavlow.ReturnScope
import io.github.sof3.enclavlow.StaticScope
import io.github.sof3.enclavlow.ThisScope
import io.github.sof3.enclavlow.ThrowScope
import io.github.sof3.enclavlow.cases.DirectLeak
import io.github.sof3.enclavlow.makeContract
import kotlin.test.Test

class ParamToReturnTests {
    @Test
    fun paramToReturn() = run<DirectLeak>("paramToReturn" to makeContract(1) {
        ParamSource(0) into ReturnScope
    })
}

class ParamToThrowTests {
    @Test
    fun paramToThrow() = run<DirectLeak>("paramToThrow" to makeContract(1) {
        ParamSource(0) into ThrowScope
    })
}

class ParamToStaticTests {
    @Test
    fun paramToStatic() = run<DirectLeak>("paramToStatic" to makeContract(1) {
        ParamSource(0) into StaticScope
    })
}

class ParamToThisTests {
    @Test
    fun paramToThis() = run<DirectLeak>("paramToThis" to makeContract(1) {
        ParamSource(0) into ThisScope
    })
}

class ThisToStaticTests {
    @Test
    fun thisToStatic() = run<DirectLeak>("thisToStatic" to makeContract(0) {
        ThisScope into StaticScope
    })
}
