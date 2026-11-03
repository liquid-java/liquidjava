package testSuite;

import liquidjava.specification.Refinement;

public class ErrorCounterexampleReturnConstant {

    @Refinement("_ > 10")
    static int ten(int port) {
        if (port < 0)
            return 0; // Expect: Refinement Error
        return 11;
    }
}
