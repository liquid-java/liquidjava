package testSuite;

import liquidjava.specification.Refinement;

public class ErrorAssignmentBeforeReturn {
    @Refinement("_ > 0")
    static int example(int x) {
        x = x + 1;
        return x; // Refinement Error
    }
}
