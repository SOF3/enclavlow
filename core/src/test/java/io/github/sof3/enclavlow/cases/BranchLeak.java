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

    public static void conditionalThrow(boolean cond, Exception e) throws Exception {
        if (cond) {
            throw e;
        }
    }

    public static int controlReset(boolean cond) {
        if (cond) {
            /* no-op */
        } else {
            /* no-op */
        }

        return 1;
    }

    // It is expected behaviour that `if(secret) return e else return e` is considered as a `secret` leak.
    // It may be too complicated to determine that this is not a real leak,
    // while most code styles would recommend moving out the return statement anyway.

    public static int switchMux(int x, int a, int b, int c, int d) {
        switch (x) {
            case 16:
                return a;
            case 17:
                return b;
            case 18:
                return c;
            default:
                return d;
        }
    }
}
