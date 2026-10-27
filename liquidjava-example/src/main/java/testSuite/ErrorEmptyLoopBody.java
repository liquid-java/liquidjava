package testSuite;

import liquidjava.specification.Refinement;

// The update of a for loop whose body is an empty statement is still checked (#395)
@SuppressWarnings("unused")
public class ErrorEmptyLoopBody {
    static void countDown() {
        @Refinement("_ >= 0")
        int n = 10;
        for (; n > 0; n -= 2) // Expect: Refinement Error
            ;
    }
}
