package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleFieldArgument {

    int timeout;

    static void connect(@Refinement("_ >= 0") int timeout) {}

    void probe(int port) {
        if (port < 0)
            return;
        connect(this.timeout); // Expect: Refinement Error
    }
}
