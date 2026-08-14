package testSuite;

import liquidjava.specification.Refinement;

public class ErrorDependentUpperBound {

    @Refinement("0 <= _ && _ < len")
    int nextIndex(
            @Refinement("_ > 0") int len,
            @Refinement("0 <= _ && _ < len") int i) {
        return i + 1; // Refinement Error
    }
}
