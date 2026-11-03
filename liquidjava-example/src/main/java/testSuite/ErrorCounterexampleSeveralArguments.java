package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleSeveralArguments {

    static void range(@Refinement("_ >= 0") int from, @Refinement("_ >= from") int to) {}

    static void probe(int port, int lo, int hi) {
        if (port < 0)
            return;
        if (lo < 0)
            return;
        range(lo, hi); // Expect: Refinement Error
    }
}
