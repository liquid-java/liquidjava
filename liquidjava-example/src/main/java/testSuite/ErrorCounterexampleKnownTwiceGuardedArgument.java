package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleKnownTwiceGuardedArgument {

    static void take(@Refinement("_ > 1") int value) {}

    static void probe(int port, int retries) {
        int value = 0;
        if (port > 0)
            if (retries < 3)
                take(value); // Expect: Refinement Error
    }
}
