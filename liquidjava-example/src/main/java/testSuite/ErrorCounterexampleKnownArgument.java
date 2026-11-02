package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleKnownArgument {

    static void window(@Refinement("_ > size") int capacity, int size) {}

    static void probe(int port, int capacity) {
        if (port < 0)
            return;
        window(capacity, 10); // Expect: Refinement Error
    }
}
