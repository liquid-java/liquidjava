package testSuite;

import liquidjava.specification.Refinement;

public class ErrorSourceStaticFinalInPredicate {

    static void requireBelowLimit(@Refinement("_ < LIMITS.MAX") double x) {
    }

    public static void main(String[] args) {
        requireBelowLimit(15.0); // Refinement Error
    }

    static class LIMITS {
        static final double MAX = 10.0;
    }
}
