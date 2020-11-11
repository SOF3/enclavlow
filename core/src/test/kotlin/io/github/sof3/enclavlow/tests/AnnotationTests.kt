package io.github.sof3.enclavlow.tests

import io.github.sof3.enclavlow.contract.CallTags
import io.github.sof3.enclavlow.cases.AnnotationCheck
import io.github.sof3.enclavlow.contract.makeContract
import kotlin.test.Test

class AnnotationTests {
    @Test
    fun enclaveCall() = testMethod<AnnotationCheck>("enclaveCall", makeContract(CallTags.ENCLAVE_CALL, 0) {})

    @Test
    fun outsideCall() = testMethod<AnnotationCheck>("outsideCall", makeContract(CallTags.OUTSIDE_CALL, 0) {})
}
