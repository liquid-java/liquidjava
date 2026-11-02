package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleBranchArgument {

    static void connect(@Refinement("_ >= 0") int timeout) {}

    static void probe(boolean secure, int port, int timeout) {
        if (secure) {
            connect(0);
        } else {
            if (port > 1024)
                connect(timeout); // Expect: Refinement Error
        }
    }
}
