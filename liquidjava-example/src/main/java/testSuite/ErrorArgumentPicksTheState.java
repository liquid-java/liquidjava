package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "filled", "closed"})
public class ErrorArgumentPicksTheState {

    @StateRefinement(to = "empty(this)")
    public ErrorArgumentPicksTheState() {}

    @StateRefinement(from = "!closed(this)", to = "keep ? filled(this) : empty(this)")
    public void reset(boolean keep) {}

    @StateRefinement(from = "filled(this)")
    public void read() {}

    public static void use() {
        ErrorArgumentPicksTheState buffer = new ErrorArgumentPicksTheState();
        buffer.reset(false);
        buffer.read(); // Expect: State Refinement Error
    }
}
