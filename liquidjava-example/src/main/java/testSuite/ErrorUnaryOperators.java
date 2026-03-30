package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorUnaryOperators {
    public static void errorUnaryOperators() {
        @Refinement("_ < 10")
        int v = 3;
        v--;
        @Refinement("_ >= 10")
        int s = 10;
        s--; // Refinement Error
    }

    public static void errorUnaryOperatorMinus() {
        @Refinement("b > 0")
        int b = 8;
        b = -b; // Refinement Error
    }
}
