package testSuite;

import liquidjava.specification.Refinement;

public class ErrorInstanceof {

    static int positive(@Refinement("_ > 0") int x) {
        return x;
    }

    static int size(Object o, @Refinement("_ >= 0") int n) {
        if (o instanceof String) {
            return positive(n); // Expect: Refinement Error
        }
        return 1;
    }
}
