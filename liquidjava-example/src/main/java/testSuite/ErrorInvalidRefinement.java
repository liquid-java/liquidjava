package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorInvalidRefinement {

    void invalidRefinement() {
        @Refinement("x") // Invalid Refinement Error
        int x = 0;
    }

    void invalidRefinementParameter(@Refinement("y + 1") int y) { // Invalid Refinement Error
    }

    @Refinement("_ * 2") // Invalid Refinement Error
    void invalidRefinementReturn() {
    }
}
