package testSuite;

import liquidjava.specification.Refinement;

public class ErrorIntegerDivision {

    @Refinement("_ > 0")
    int half(@Refinement("_ > 0") int x) {
        return x / 2; // Refinement Error
    }
}
