package io.github.sof3.enclavlow

import io.github.sof3.enclavlow.cases.DirectAssignLeak
import kotlin.test.Test

class Run {
    @Test
    fun simpleAssign() = run<DirectAssignLeak>()
}
