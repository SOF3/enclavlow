package io.github.sof3.enclavlow.tests.lfg

import io.github.sof3.enclavlow.cases.lfg.ProjectionCase
import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.contract.MethodControlNode
import io.github.sof3.enclavlow.contract.ParamLocalNode
import io.github.sof3.enclavlow.contract.ReturnLocalNode
import io.github.sof3.enclavlow.contract.StaticLocalNode
import io.github.sof3.enclavlow.contract.ThisLocalNode
import io.github.sof3.enclavlow.contract.makeContract
import io.github.sof3.enclavlow.tests.testMethod
import kotlin.test.Test

class ProjectionTests {
    @Test
    fun paramToThis() = testMethod<ProjectionCase>("paramToThis", makeContract(CallTags.UNSPECIFIED, 1) {
        MethodControlNode into ReturnLocalNode
        MethodControlNode into projection(ThisLocalNode, "int b")
        ParamLocalNode(0) into projection(ThisLocalNode, "int b")
    })

    @Test
    fun thisToStatic() = testMethod<ProjectionCase>("thisToStatic", makeContract(CallTags.UNSPECIFIED, 0) {
        MethodControlNode into ReturnLocalNode
        MethodControlNode into StaticLocalNode
        projection(ThisLocalNode, "int b") into StaticLocalNode
    })

    @Test
    fun paramToParam() = testMethod<ProjectionCase>("paramToParam", makeContract(CallTags.UNSPECIFIED, 2) {
        MethodControlNode into ReturnLocalNode
        MethodControlNode into projection(ParamLocalNode(1), "public java.lang.Object inner")
        ParamLocalNode(0) into projection(ParamLocalNode(1), "public java.lang.Object inner")
    })
}
