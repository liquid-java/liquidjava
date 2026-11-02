package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// read() accepts two states of one set. The other set is not named, and no call reaches either state from any state
@StateSet({"created", "open", "ready"})
@StateSet({"quiet", "loud"})
public class ErrorStateHintUnknownDisjunction {

    @StateRefinement(to = "created(this) && quiet(this)")
    public ErrorStateHintUnknownDisjunction() {}

    @StateRefinement(from = "created(this)", to = "open(this)")
    public void open() {}

    @StateRefinement(from = "open(this) || ready(this)")
    public void read() {}

    @StateRefinement(to = "loud(this)")
    public void shout() {}

    public static void use(ErrorStateHintUnknownDisjunction s) {
        s.read(); // Expect: State Refinement Error
    }
}
