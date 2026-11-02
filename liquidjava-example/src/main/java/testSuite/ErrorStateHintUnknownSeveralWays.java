package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// go() accepts s1 or s2, and a different call reaches each of them from any state
@StateSet({"s0", "s1", "s2", "s3"})
public class ErrorStateHintUnknownSeveralWays {

    @StateRefinement(to = "s0(this)")
    public ErrorStateHintUnknownSeveralWays() {}

    @StateRefinement(to = "s1(this)")
    public void one() {}

    @StateRefinement(to = "s2(this)")
    public void two() {}

    @StateRefinement(to = "s3(this)")
    public void three() {}

    @StateRefinement(from = "s1(this) || s2(this)")
    public void go() {}

    public static void use(ErrorStateHintUnknownSeveralWays x) {
        x.go(); // Expect: State Refinement Error
    }
}
