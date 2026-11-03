package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The constructor creates the needed state, but the class is abstract, so no new object can be suggested
@StateSet({"fresh", "ready"})
public abstract class ErrorStateHintUnknownAbstract {

    @StateRefinement(to = "ready(this)")
    public ErrorStateHintUnknownAbstract() {}

    @StateRefinement(from = "ready(this)", to = "fresh(this)")
    public void use() {}

    static void fail(ErrorStateHintUnknownAbstract value) {
        value.use(); // Expect: State Refinement Error
    }
}
