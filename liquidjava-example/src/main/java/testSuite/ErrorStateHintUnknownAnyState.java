package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// reset() works in any state, so it is suggested even though the state is unknown; open() alone is not
@StateSet({"created", "open", "closed"})
public class ErrorStateHintUnknownAnyState {

    @StateRefinement(to = "created(this)")
    public ErrorStateHintUnknownAnyState() {}

    @StateRefinement(to = "created(this)")
    public void reset() {}

    @StateRefinement(from = "created(this)", to = "open(this)")
    public void open() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    public static void use(ErrorStateHintUnknownAnyState f) {
        f.read(); // Expect: State Refinement Error
    }
}
