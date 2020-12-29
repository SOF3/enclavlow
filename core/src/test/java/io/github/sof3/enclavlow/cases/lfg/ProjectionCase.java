package io.github.sof3.enclavlow.cases.lfg;

import io.github.sof3.enclavlow.cases.Box;

@SuppressWarnings("unused")
public class ProjectionCase {
    static int a;
    int b;

    public void paramToThis(int x) {
        b = x;
    }

    public void thisToStatic() {
        a = b;
    }

    public static void paramToParam(String x, Box<String> y) {
        y.inner = x;
    }
}
