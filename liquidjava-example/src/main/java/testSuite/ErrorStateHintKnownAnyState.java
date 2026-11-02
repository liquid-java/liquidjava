package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The class of ErrorStateHintUnknownAnyState with a known state: the hint starts from that state
@StateSet({"created", "open", "closed"})
public class ErrorStateHintKnownAnyState {

    @StateRefinement(to = "created(this)")
    public ErrorStateHintKnownAnyState() {}

    @StateRefinement(to = "created(this)")
    public void reset() {}

    @StateRefinement(from = "created(this)", to = "open(this)")
    public void open() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    public static void main(String[] args) {
        ErrorStateHintKnownAnyState f = new ErrorStateHintKnownAnyState();
        f.open();
        f.close();
        f.read(); // Expect: State Refinement Error
    }
}
