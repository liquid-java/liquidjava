package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleGuardRelatesArguments {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(int port, int retries, int timeout) {
        if (port < 0 || retries > timeout + 5)
            return;
        connect(timeout); // Expect: Refinement Error
    }
}
