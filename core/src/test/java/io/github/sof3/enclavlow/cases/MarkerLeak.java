package io.github.sof3.enclavlow.cases;

import static io.github.sof3.enclavlow.api.Enclavlow.intSinkMarker;
import static io.github.sof3.enclavlow.api.Enclavlow.intSourceMarker;

public class MarkerLeak {
    public static int returnSource() {
        return intSourceMarker(1);
    }

    public static int returnSink(int x) {
        return intSinkMarker(x);
    }
}
