package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The paths differ in one of two state sets, so the states are named in both sets the call checks
@StateSet({"closed", "opened"})
@StateSet({"unlocked", "locked"})
public class ErrorStateHintJoinOneOfTwoSets {

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public ErrorStateHintJoinOneOfTwoSets() {}

    @StateRefinement(from = "closed(this)", to = "opened(this)")
    public void open() {}

    @StateRefinement(from = "unlocked(this)", to = "locked(this)")
    public void lock() {}

    @StateRefinement(from = "closed(this) && unlocked(this)")
    public void enter() {}

    public static void use(boolean p) {
        ErrorStateHintJoinOneOfTwoSets d = new ErrorStateHintJoinOneOfTwoSets();
        if (p)
            d.open();
        d.enter(); // Expect: State Refinement Error
    }
}
