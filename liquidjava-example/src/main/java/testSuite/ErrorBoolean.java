package testSuite;

import liquidjava.specification.Refinement;

public class ErrorBoolean {

    @Refinement("_ == true")
    boolean mustBeTrue(boolean value) {
        return value; // Refinement Error
    }
}
