package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

@Ghost("boolean ready")
public class ErrorUnconstrainedStateRefinement {

    @StateRefinement(from = "ready(this)")
    public void run() {}

    public static void check(ErrorUnconstrainedStateRefinement value) {
        value.run(); // State Refinement Error
    }
}
