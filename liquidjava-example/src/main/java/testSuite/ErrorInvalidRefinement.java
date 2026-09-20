package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorInvalidRefinement {

    void invalidRefinement() {
        @Refinement("x") // Expect: Invalid Refinement Error
        int x = 0;
    }

    void invalidRefinementParameter(@Refinement("y + 1") int y) { // Expect: Invalid Refinement Error
    }

    @Refinement("_ * 2") // Expect: Invalid Refinement Error
    void invalidRefinementReturn() {
    }
}
