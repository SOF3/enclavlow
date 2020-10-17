package io.github.sof3.enclavlow.cases;

import io.github.sof3.enclavlow.api.Source;

public class DirectAssignLeak {
    public static Secret identity(@Source Secret secret1, @Source Secret secret2) {
        secret1.value += secret2.value;
        return secret1;
    }

    static class Secret {
        int value;
    }
}
