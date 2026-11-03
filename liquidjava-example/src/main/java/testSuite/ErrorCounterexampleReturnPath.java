package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleReturnPath {

    @Refinement("_ >= 0")
    static int timeoutOf(int port, int configured) {
        if (port < 0)
            return 0;
        return configured; // Expect: Refinement Error
    }
}
