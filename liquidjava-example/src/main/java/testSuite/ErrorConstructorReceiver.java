package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"fresh", "used"})
class ErrorReceiverToken {
    @StateRefinement(to = "fresh(this)")
    public ErrorReceiverToken() {}

    @StateRefinement(to = "used(this)")
    public ErrorReceiverToken(int spent) {}

    @StateRefinement(from = "fresh(this)", to = "used(this)")
    public void use() {}

    @StateRefinement(from = "used(this)")
    public void report() {}
}

public class ErrorConstructorReceiver {
    static void chained() {
        new ErrorReceiverToken(1).use(); // Expect: State Refinement Error
    }
}
