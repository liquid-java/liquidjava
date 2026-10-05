package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

public class ErrorRefinementDeclarationPositions {
    @Refinement("_ > 10")
    private int field = 11;

    @StateRefinement(from = "true", to = "true")
    @Refinement(value = "_ > 20", msg = "result must exceed twenty")
    int result() {
        return 0; // Expect: Refinement Error
    }

    void parameter(@Refinement("_ > 30") int value) {
    }

    void check() {
        parameter(0); // Expect: Refinement Error
    }

    void local() {
        @Refinement("_ > 40")
        int local = 41;
        local = 0; // Expect: Refinement Error
    }

    void field() {
        field = 0; // Expect: Refinement Error
    }
}
