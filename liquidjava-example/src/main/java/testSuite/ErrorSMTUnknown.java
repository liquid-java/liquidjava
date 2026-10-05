package testSuite;

import liquidjava.specification.Refinement;

public class ErrorSMTUnknown {
    @Refinement("_ > 2.0")
    public static int aboveTwo(int value) {
        return value; // Expect: SMT Unknown Error
    }
}
