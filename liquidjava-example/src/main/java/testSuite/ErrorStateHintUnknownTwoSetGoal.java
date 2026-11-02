package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// open() checks both state sets, so both are named. reset() reaches them from any state; slam() fixes only one set
@StateSet({"closed", "opened"})
@StateSet({"unlocked", "locked"})
public class ErrorStateHintUnknownTwoSetGoal {

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public ErrorStateHintUnknownTwoSetGoal() {}

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public void reset() {}

    @StateRefinement(to = "closed(this)")
    public void slam() {}

    @StateRefinement(from = "closed(this) && unlocked(this)", to = "opened(this)")
    public void open() {}

    @StateRefinement(from = "locked(this)", to = "unlocked(this)")
    public void unlock() {}

    public static void enter(ErrorStateHintUnknownTwoSetGoal door) {
        door.open(); // Expect: State Refinement Error
    }
}
