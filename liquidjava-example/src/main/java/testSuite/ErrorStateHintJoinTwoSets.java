package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Every combination of two state sets is reached on some path: a join of every state, not a lack of knowledge
@StateSet({"closed", "opened"})
@StateSet({"unlocked", "locked"})
public class ErrorStateHintJoinTwoSets {

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public ErrorStateHintJoinTwoSets() {}

    @StateRefinement(from = "closed(this)", to = "opened(this)")
    public void open() {}

    @StateRefinement(from = "unlocked(this)", to = "locked(this)")
    public void lock() {}

    @StateRefinement(from = "closed(this) && unlocked(this)")
    public void enter() {}

    public static void use(boolean p, boolean q) {
        ErrorStateHintJoinTwoSets d = new ErrorStateHintJoinTwoSets();
        if (p)
            d.open();
        if (q)
            d.lock();
        d.enter(); // Expect: State Refinement Error
    }
}
