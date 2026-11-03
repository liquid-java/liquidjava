package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleLinkedPathCondition {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(int port, int retries) {
        if (retries > 3)
            return;
        if (port < 0)
            return;
        connect(port - 1); // Expect: Refinement Error
    }
}
