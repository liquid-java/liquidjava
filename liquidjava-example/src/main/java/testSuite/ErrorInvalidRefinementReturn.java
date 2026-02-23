// Invalid Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorInvalidRefinementReturn {

    @Refinement("_ * 2")
    void test() {
    }
}
