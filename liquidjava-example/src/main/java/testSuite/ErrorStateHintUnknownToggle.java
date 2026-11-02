package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// flip() is allowed in every state, but by a different state refinement in each, so an unknown state rejects it
@StateSet({"up", "down"})
public class ErrorStateHintUnknownToggle {

    @StateRefinement(to = "up(this)")
    public ErrorStateHintUnknownToggle() {}

    @StateRefinement(from = "up(this)", to = "down(this)")
    @StateRefinement(from = "down(this)", to = "up(this)")
    public void flip() {}

    public static void use(ErrorStateHintUnknownToggle t) {
        t.flip(); // Expect: State Refinement Error
    }
}
