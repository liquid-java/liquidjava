package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// read() accepts a state of either set, so each is named alone, not combined with every state of the other set
@StateSet({"created", "open", "ready"})
@StateSet({"quiet", "loud"})
public class ErrorStateHintUnknownCrossDisjunction {

    @StateRefinement(to = "created(this) && quiet(this)")
    public ErrorStateHintUnknownCrossDisjunction() {}

    @StateRefinement(from = "open(this) || loud(this)")
    public void read() {}

    public static void use(ErrorStateHintUnknownCrossDisjunction s) {
        s.read(); // Expect: State Refinement Error
    }
}
