package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Each check of a loop body starts from the same facts: the values left by an earlier check must not leak into it
@StateSet({"open", "closed"})
public class ErrorLoopStatePasses {
    @StateRefinement(to = "open(this)")
    ErrorLoopStatePasses() {}

    @StateRefinement(to = "closed(this)")
    void close() {}

    @StateRefinement(from = "closed(this)", to = "open(this)")
    void reopen() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    @StateRefinement(from = "closed(this)", to = "open(this)")
    void toggle() {}

    // the loop is never entered, so b is still open; x == 0 from the previous check must not make the exit impossible
    static void neverEntered() {
        ErrorLoopStatePasses b = new ErrorLoopStatePasses();
        int x = 100;
        while (x < 10) {
            b.close();
            x = 0;
        }
        b.reopen(); // Expect: State Refinement Error
    }

    // in the first iteration y == 0; x == 7 from the previous check must not hide the error in the lambda
    static void lambdaInFirstIteration(int n) {
        ErrorLoopStatePasses t = new ErrorLoopStatePasses();
        int x = 0;
        while (n > 0) {
            t.toggle();
            final int y = x;
            Runnable r = () -> {
                @Refinement("_ == 7")
                int z = y; // Expect: Refinement Error
            };
            x = 7;
            n--;
        }
    }
}
