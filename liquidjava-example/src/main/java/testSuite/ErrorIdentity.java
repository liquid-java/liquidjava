package testSuite;

import liquidjava.specification.Refinement;

public class ErrorIdentity {

    @Refinement("_ > 0")
    int positiveIdentity(int x) {
        return x; // Expect: Refinement Error
    }
}
