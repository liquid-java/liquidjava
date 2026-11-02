package testSuite;

import liquidjava.specification.Refinement;

// #453: found knows nothing about timeout, and the port guard does not affect it
public class ErrorCounterexampleUnconstrainedArgument {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(int port, int timeout) {
        if (port < 0 || port > 65535)
            return;
        connect(timeout); // Expect: Refinement Error
    }
}
