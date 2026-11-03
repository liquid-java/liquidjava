package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A loop body checked more than once creates fresh receivers in each check, not reusing one of an earlier check
@StateSet({"open", "marked", "closed"})
public class ErrorLoopFreshReceivers {
    @StateRefinement(to = "open(this)")
    ErrorLoopFreshReceivers() {}

    @StateRefinement(from = "open(this)", to = "marked(this)")
    @StateRefinement(from = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "!closed(this)", to = "closed(this)")
    void close() {}

    // a fresh object is open, and reset needs it marked
    static void freshReceiverInLambda(int n) {
        ErrorLoopFreshReceivers b = new ErrorLoopFreshReceivers();
        while (n > 0) {
            b.mark(); // the body is checked from open, then from open or marked
            Runnable r = () -> new ErrorLoopFreshReceivers().reset(); // Expect: State Refinement Error
            n--;
        }
    }
}
