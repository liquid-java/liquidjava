package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleGuardedArgument {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(int port, int timeout) {
        if (timeout > 100)
            return;
        if (port < 0)
            return;
        connect(timeout); // Expect: Refinement Error
    }
}
