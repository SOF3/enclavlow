package io.github.sof3.enclavlow.cases;

import edu.hku.cs.uranus.IntelSGXOcall;

@SuppressWarnings("unused")
public class LoopLeak {
    public static int loopAssign(int i) {
        int a = 0;
        for (int j = 0; j < i; j++) {
            a += j;
        }
        return a;
    }
}
