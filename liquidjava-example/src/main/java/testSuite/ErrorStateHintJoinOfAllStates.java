package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"created", "open"})
public class ErrorStateHintJoinOfAllStates {

    @StateRefinement(to = "created(this)")
    public ErrorStateHintJoinOfAllStates() {}

    @StateRefinement(from = "created(this)", to = "open(this)")
    public void open() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    public static void use(boolean ready) {
        ErrorStateHintJoinOfAllStates f = new ErrorStateHintJoinOfAllStates();
        if (ready)
            f.open();
        f.read(); // Expect: State Refinement Error
    }
}
