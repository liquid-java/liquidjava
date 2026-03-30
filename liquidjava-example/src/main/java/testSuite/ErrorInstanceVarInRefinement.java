package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorInstanceVarInRefinement {
    public static void varInRefinementInIf() {
        @Refinement("_ < 10")
        int a = 6;
        if (a > 0) {
            a = -2;
            @Refinement("b < a")
            int b = -3; // Refinement Error
        }
    }

    public static void varInRefinement() {
        @Refinement("_ < 10")
        int a = 6;

        @Refinement("_ > a")
        int b = 9; // Refinement Error
    }
}
