package testSuite;

import liquidjava.specification.Refinement;

public class ErrorWarningUnsatRefinement {

    public void example1() {
        @Refinement("x == 1 && x != 1") // Unsat Refinement Warning
        int x = 1; // Refinement Error
    }

    public void example2() {
        @Refinement("x % 2 > 1") // Unsat Refinement Warning
        int x = 5; // Refinement Error
    }

    public void example3() {
        @Refinement("false") // Unsat Refinement Warning
        int x = 0; // Refinement Error
    }

    public void example4(@Refinement("x > 0 && x < 0") int x) {} // Unsat Refinement Warning

    @Refinement("_ == true && _ == false") // Unsat Refinement Warning
    public boolean example5() {
        return true; // Refinement Error
    }
}
