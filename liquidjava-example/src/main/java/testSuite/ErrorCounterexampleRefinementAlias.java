package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@RefinementAlias("NonNegative(int v) { v >= 0 }")
public class ErrorCounterexampleRefinementAlias {

    static void connect(@Refinement("NonNegative(_)") int timeout) {}

    static void probe(int port, int timeout) {
        if (port < 0)
            return;
        connect(timeout); // Expect: Refinement Error
    }
}
