package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleKnownAliasedArgument {

    static void take(@Refinement("_ > 1") int value) {}

    static void probe(int port) {
        int base = 0;
        int value = base;
        if (port > 0)
            take(value); // Expect: Refinement Error
    }
}
