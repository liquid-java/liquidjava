package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorStaticFinalConstant {

    static void requirePositive(@Refinement("_ > 0") int x) {
    }

    public static void main(String[] args) {
        requirePositive(Integer.MIN_VALUE); // Refinement Error
    }
}
