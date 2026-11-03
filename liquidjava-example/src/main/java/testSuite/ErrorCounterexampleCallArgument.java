package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleCallArgument {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static int read(int key) {
        return key;
    }

    static void probe(int port, int key) {
        if (port < 0)
            return;
        connect(read(key) - 1); // Expect: Refinement Error
    }
}
