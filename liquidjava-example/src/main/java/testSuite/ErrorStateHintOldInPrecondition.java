package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"created", "open"})
public class ErrorStateHintOldInPrecondition {

    @StateRefinement(to = "created(this)")
    public ErrorStateHintOldInPrecondition() {}

    @StateRefinement(from = "created(this)", to = "open(this)")
    public void open() {}

    @StateRefinement(from = "open(old(this))")
    public void read() {}

    public static void main(String[] args) {
        ErrorStateHintOldInPrecondition f = new ErrorStateHintOldInPrecondition();
        f.read(); // Expect: State Refinement Error
    }
}
