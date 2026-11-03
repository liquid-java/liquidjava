package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleReturnFromPath {

    @Refinement("_ > 10")
    static int ten(int port) {
        if (port < 0)
            return port + 1; // Expect: Refinement Error
        return 11;
    }
}
