package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A field whose state is unknown: forceOpen() works in any state, and only the state set read() checks is named
@StateSet({"closed", "opened"})
@StateSet({"unlocked", "locked"})
public class ErrorStateHintUnknownField {

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public ErrorStateHintUnknownField() {}

    @StateRefinement(to = "opened(this)")
    public void forceOpen() {}

    @StateRefinement(from = "opened(this)")
    public void read() {}

    @StateRefinement(from = "locked(this)", to = "unlocked(this)")
    public void unlock() {}
}

class ErrorStateHintUnknownFieldUser {
    ErrorStateHintUnknownField door;

    void use() {
        door.read(); // Expect: State Refinement Error
    }
}
