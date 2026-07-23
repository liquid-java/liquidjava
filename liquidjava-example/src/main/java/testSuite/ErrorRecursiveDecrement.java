package testSuite;

import liquidjava.specification.Refinement;

public class ErrorRecursiveDecrement {

    public int f(@Refinement("_ > 0") int x) {
        return f(x - 1); // Refinement Error
    }
}
