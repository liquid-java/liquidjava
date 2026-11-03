package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Joins of the states reached on different paths are not a lack of knowledge
@StateSet({"closed", "opened"})
@StateSet({"unlocked", "locked"})
public class ErrorStateHintJoins {

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public ErrorStateHintJoins() {}

    @StateRefinement(from = "closed(this)", to = "opened(this)")
    public void open() {}

    @StateRefinement(from = "unlocked(this)", to = "locked(this)")
    public void lock() {}

    @StateRefinement(from = "closed(this) && unlocked(this)")
    public void enter() {}

    // every combination of the two state sets is reached on some path
    public static void everyState(boolean p, boolean q) {
        ErrorStateHintJoins d = new ErrorStateHintJoins();
        if (p)
            d.open();
        if (q)
            d.lock();
        d.enter(); // Expect: State Refinement Error
    }

    // the paths differ in one of the two state sets the call checks, so both states are named in full
    public static void oneSet(boolean p) {
        ErrorStateHintJoins d = new ErrorStateHintJoins();
        if (p)
            d.open();
        d.enter(); // Expect: State Refinement Error
    }
}
