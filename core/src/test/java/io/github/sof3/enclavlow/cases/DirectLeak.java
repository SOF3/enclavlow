package io.github.sof3.enclavlow.cases;

@SuppressWarnings("unused")
public class DirectLeak {
    public static int paramToReturn(int x) {
        return x;
    }

    public static int paramToThrow(RuntimeException x) {
        throw x;
    }

    static int a;
    int b;

    public static void paramToStatic(int x) {
        a = x;
    }

    public void paramToThis(int x) {
        b = x;
    }

    public void thisToStatic() {
        a = b;
    }
}
