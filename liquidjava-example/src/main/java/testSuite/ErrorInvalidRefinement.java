// Invalid Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorInvalidRefinement {

    void test() {
        @Refinement("x")
        int x = 0;
    }
}
