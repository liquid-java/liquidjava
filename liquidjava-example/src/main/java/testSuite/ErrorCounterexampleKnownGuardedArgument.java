package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleKnownGuardedArgument {

    static void take(@Refinement("_ > 1") int value) {}

    static void probe(int port) {
        int value = 0;
        if (port > 0)
            take(value); // Expect: Refinement Error
    }
}
