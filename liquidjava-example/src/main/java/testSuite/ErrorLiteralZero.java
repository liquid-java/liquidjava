package testSuite;

import liquidjava.specification.Refinement;

public class ErrorLiteralZero {

    @Refinement("_ != 0")
    int zero() {
        return 0; // Refinement Error
    }
}
