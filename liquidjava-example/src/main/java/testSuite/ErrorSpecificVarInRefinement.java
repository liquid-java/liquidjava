// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSpecificVarInRefinement {
    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 6;

        @Refinement("_ > a")
        int b = 9;
    }
}
