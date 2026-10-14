package testSuite;

import liquidjava.specification.Refinement;

// #322: Limits.max must not be read as this class's own field max
public class ErrorFieldOfOtherClass {
    int max = 5;

    void check() {
        @Refinement("_ == 5")
        int k = Limits.max; // Expect: Refinement Error
    }

    static class Limits {
        static final int max = 10;
    }
}
