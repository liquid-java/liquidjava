package testSuite;

import liquidjava.specification.Refinement;

public class ErrorUnconstrainedRefinement {

    private static void requirePositive(@Refinement("_ > 0") int value) {}

    public static void check(int value) {
        requirePositive(value); // Refinement Error
    }
}
