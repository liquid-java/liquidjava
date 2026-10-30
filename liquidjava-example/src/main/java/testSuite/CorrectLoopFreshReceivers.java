package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A loop body checked more than once creates fresh receivers in each check, not reusing one of an earlier check
@StateSet({"open", "marked", "closed"})
public class CorrectLoopFreshReceivers {
    @StateRefinement(to = "open(this)")
    CorrectLoopFreshReceivers() {}

    @StateRefinement(from = "open(this)", to = "marked(this)")
    @StateRefinement(from = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "!closed(this)", to = "closed(this)")
    void close() {}

    static void freshReceivers(int n) {
        CorrectLoopFreshReceivers b = new CorrectLoopFreshReceivers();
        while (n > 0) {
            b.mark(); // the body is checked from open, then from open or marked
            new CorrectLoopFreshReceivers().close();
            new CorrectLoopFreshReceivers().mark();
            Runnable r = () -> new CorrectLoopFreshReceivers().close();
            n--;
        }
    }
}
