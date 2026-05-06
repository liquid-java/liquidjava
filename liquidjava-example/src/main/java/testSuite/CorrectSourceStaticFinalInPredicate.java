package testSuite;

import liquidjava.specification.Refinement;

public class CorrectSourceStaticFinalInPredicate {

    static void requireBelowLimit(@Refinement("_ < LIMITS.MAX") double x) {
    }

    public static void main(String[] args) {
        requireBelowLimit(5.0);
    }

    static class LIMITS {
        static final double MAX = 10.0;
    }
}
