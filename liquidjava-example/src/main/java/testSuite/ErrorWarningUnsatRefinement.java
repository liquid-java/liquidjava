package testSuite;

import liquidjava.specification.Refinement;

public class ErrorWarningUnsatRefinement {

    public void example1() {
        @Refinement("x == 1 && x != 1") // Expect: Unsat Refinement Warning
        int x = 1; // Expect: Refinement Error
    }

    public void example2() {
        @Refinement("x % 2 > 1") // Expect: Unsat Refinement Warning
        int x = 5; // Expect: Refinement Error
    }

    public void example3() {
        @Refinement("false") // Expect: Unsat Refinement Warning
        int x = 0; // Expect: Refinement Error
    }

    public void example4(@Refinement("x > 0 && x < 0") int x) {} // Expect: Unsat Refinement Warning

    @Refinement("_ == true && _ == false") // Expect: Unsat Refinement Warning
    public boolean example5() {
        return true; // Expect: Refinement Error
    }
}
