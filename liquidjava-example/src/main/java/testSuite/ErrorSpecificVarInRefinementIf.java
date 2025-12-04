// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSpecificVarInRefinementIf {
    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 6;
        if (a > 0) {
            a = -2;
            @Refinement("b < a")
            int b = -3;
        }
    }
}
