package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleMixedGuard {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(int port, int retries, int backoff, int timeout) {
        if (port < 0 || (retries > 3 && backoff > 10) || timeout > 60000)
            return;
        connect(timeout); // Expect: Refinement Error
    }
}
