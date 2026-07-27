package testSuite;

import liquidjava.specification.Refinement;

public class ErrorRecursiveSiblingParameter {

    int fibonacci(@Refinement("_ > 0") int n) {
        if (n == 1)
            return 1;
        else
            return fibonacci(n - 1) + fibonacci(n - 2); // Refinement Error
    }

    int factorial(@Refinement("_ > 0") int n) {
        return n * factorial(n - 1); // Refinement Error
    }
}
