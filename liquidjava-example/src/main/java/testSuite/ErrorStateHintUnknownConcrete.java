package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The same class as ErrorStateHintUnknownAbstract, but concrete: the constructor is suggested
@StateSet({"fresh", "ready"})
public class ErrorStateHintUnknownConcrete {

    @StateRefinement(to = "ready(this)")
    public ErrorStateHintUnknownConcrete() {}

    @StateRefinement(from = "ready(this)", to = "fresh(this)")
    public void use() {}

    static void fail(ErrorStateHintUnknownConcrete value) {
        value.use(); // Expect: State Refinement Error
    }
}
