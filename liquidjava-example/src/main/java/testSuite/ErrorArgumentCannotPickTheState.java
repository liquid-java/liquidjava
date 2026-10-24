package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "filled", "closed"})
public class ErrorArgumentCannotPickTheState {

    @StateRefinement(to = "empty(this)")
    public ErrorArgumentCannotPickTheState() {}

    @StateRefinement(from = "!closed(this)", to = "keep ? filled(this) : empty(this)")
    public void reset(boolean keep) {}

    @StateRefinement(from = "closed(this)")
    public void reopen() {}

    // no value of keep closes the object, so the argument is not what went wrong
    public static void use() {
        ErrorArgumentCannotPickTheState buffer = new ErrorArgumentCannotPickTheState();
        buffer.reset(true);
        buffer.reopen(); // Expect: State Refinement Error
    }
}
