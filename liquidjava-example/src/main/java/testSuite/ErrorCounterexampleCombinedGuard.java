package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleCombinedGuard {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(int port, int retries, int backoff, int window, int timeout) {
        if (port < 0 || port > 65535 || retries < 0 || backoff < 0 || window < 1 || timeout > 60000)
            return;
        connect(timeout); // Expect: Refinement Error
    }
}
