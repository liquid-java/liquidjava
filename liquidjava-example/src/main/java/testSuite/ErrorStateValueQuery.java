package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// a query written with its state set is offered as a guard, like one written with the state
@StateSet(name = "lock", value = {"unlocked", "locked"})
public abstract class ErrorStateValueQuery {

    @StateRefinement(to = "lock(this) == locked")
    public ErrorStateValueQuery() {}

    @StateRefinement(from = "lock(this) == unlocked")
    public void open() {}

    @Refinement("_ == true --> lock(this) == unlocked")
    public abstract boolean isUnlocked();

    static void use(ErrorStateValueQuery d) {
        d.open(); // Expect: State Refinement Error
    }
}
