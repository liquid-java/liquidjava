package testSuite;

import liquidjava.specification.Refinement;

public class ErrorStaticFinalCharInPredicate {

    static void requireMaxChar(@Refinement("_ == Character.MAX_VALUE") char c) {
    }

    public static void main(String[] args) {
        requireMaxChar('\''); // Refinement Error
    }
}
