package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"a", "b", "c"})
public class ErrorStateHintInheritedStates {

    @StateRefinement(to = "a(this)")
    public ErrorStateHintInheritedStates() {}

    @StateRefinement(from = "a(this)", to = "b(this)")
    public void ab() {}

    @StateRefinement(from = "c(this)")
    public void useC() {}

    public static void use() {
        ErrorStateHintInheritedStatesChild s = new ErrorStateHintInheritedStatesChild();
        s.ab();
        s.useC(); // Expect: State Refinement Error
    }
}

// adds a transition to the states it inherits
class ErrorStateHintInheritedStatesChild extends ErrorStateHintInheritedStates {

    @StateRefinement(from = "b(this)", to = "c(this)")
    public void bc() {}
}
