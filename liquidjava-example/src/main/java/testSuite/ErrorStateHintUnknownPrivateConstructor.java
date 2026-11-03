package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The constructor creates the needed state, but it is private, so no new object can be suggested
@StateSet({"fresh", "ready"})
public class ErrorStateHintUnknownPrivateConstructor {

    @StateRefinement(to = "ready(this)")
    private ErrorStateHintUnknownPrivateConstructor() {}

    @StateRefinement(from = "ready(this)", to = "fresh(this)")
    public void use() {}
}

class ErrorStateHintUnknownPrivateConstructorUser {
    static void fail(ErrorStateHintUnknownPrivateConstructor value) {
        value.use(); // Expect: State Refinement Error
    }
}
