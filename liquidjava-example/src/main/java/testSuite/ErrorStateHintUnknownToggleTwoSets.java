package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Every state allows flip(), by the refinement for a or the one for b, so the hint names a or b, not all six states
@StateSet({"a", "b"})
@StateSet({"x", "y", "z"})
public class ErrorStateHintUnknownToggleTwoSets {

    @StateRefinement(to = "a(this) && x(this)")
    public ErrorStateHintUnknownToggleTwoSets() {}

    @StateRefinement(from = "a(this)", to = "b(this)")
    @StateRefinement(from = "b(this)", to = "a(this)")
    public void flip() {}

    public static void use(ErrorStateHintUnknownToggleTwoSets t) {
        t.flip(); // Expect: State Refinement Error
    }
}
