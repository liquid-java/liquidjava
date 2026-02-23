// Invalid Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorInvalidRefinementParameter {

    void testInvalidRefinement(@Refinement("y + 1") int y) {}

    int otherMethod() {
        return -1;
    }
}
