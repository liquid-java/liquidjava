package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The receiver is this, so creating a new object, though it would start in open, is not suggested
@StateSet({"open", "closed"})
public class ErrorStateHintUnknownThis {

    @StateRefinement(to = "open(this)")
    public ErrorStateHintUnknownThis() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    public void helper() {
        this.read(); // Expect: State Refinement Error
    }
}
