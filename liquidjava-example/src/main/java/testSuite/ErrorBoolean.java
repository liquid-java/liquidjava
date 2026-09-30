package testSuite;

import liquidjava.specification.Refinement;

public class ErrorBoolean {

    @Refinement("_ == true")
    boolean mustBeTrue(boolean value) {
        return value; // Expect: Refinement Error
    }
}
