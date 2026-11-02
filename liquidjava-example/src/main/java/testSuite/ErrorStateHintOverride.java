package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"a", "b", "c"})
public class ErrorStateHintOverride {

    @StateRefinement(to = "a(this)")
    public ErrorStateHintOverride() {}

    @StateRefinement(from = "a(this)", to = "b(this) || c(this)")
    public void next() {}

    @StateRefinement(from = "c(this)")
    public void useC() {}

    public static void use() {
        ErrorStateHintOverrideChild s = new ErrorStateHintOverrideChild();
        s.useC(); // Expect: State Refinement Error
    }
}

// the override always moves to c, which the superclass's refinement does not promise
class ErrorStateHintOverrideChild extends ErrorStateHintOverride {

    @Override
    @StateRefinement(from = "a(this)", to = "c(this)")
    public void next() {}
}
