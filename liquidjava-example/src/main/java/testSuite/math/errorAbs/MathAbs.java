package testSuite.math.errorAbs;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class MathAbs {
    public static void main(String[] args) {
        @Refinement("true")
        int ab = Math.abs(-9);

        @Refinement("_ == 9")
        int ab1 = -ab; // Refinement Error
    }
}
