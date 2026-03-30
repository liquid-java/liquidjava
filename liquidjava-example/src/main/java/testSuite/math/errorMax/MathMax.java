package testSuite.math.errorMax;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class MathMax {
    public static void main(String[] args) {
        @Refinement("true")
        int ab = Math.abs(-9);

        @Refinement("_ == 9")
        int ab1 = Math.max(-9, -ab); // Refinement Error
    }
}
