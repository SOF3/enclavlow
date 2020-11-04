package io.github.sof3.enclavlow.cases;

import edu.hku.cs.uranus.IntelSGX;
import edu.hku.cs.uranus.IntelSGXOcall;

public class AnnotationCheck {
    @IntelSGX
    static void enclaveCall() {
    }

    @IntelSGXOcall
    static void outsideCall() {
    }
}
