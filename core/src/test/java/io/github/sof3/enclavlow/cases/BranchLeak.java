package io.github.sof3.enclavlow.cases;

@SuppressWarnings("unused")
public class BranchLeak {
    public static int conditionalAssign(boolean cond) {
        int ret = 3;
        if (cond) {
            ret %= 2;
        }
        return ret;
    }
}
