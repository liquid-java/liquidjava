package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Each constructor creates the needed state, but only one that can be called is suggested
@StateSet({"fresh", "ready"})
public class ErrorStateHintConstructors {

    @StateRefinement(to = "ready(this)")
    public ErrorStateHintConstructors() {}

    @StateRefinement(from = "ready(this)", to = "fresh(this)")
    public void use() {}

    // a concrete class: the constructor is suggested
    static void concrete(ErrorStateHintConstructors value) {
        value.use(); // Expect: State Refinement Error
    }

    // the receiver is this object, which a new object would not change
    public void self() {
        this.use(); // Expect: State Refinement Error
    }
}

@StateSet({"fresh", "ready"})
abstract class ErrorStateHintConstructorsAbstract {

    @StateRefinement(to = "ready(this)")
    public ErrorStateHintConstructorsAbstract() {}

    @StateRefinement(from = "ready(this)", to = "fresh(this)")
    public void use() {}

    static void fail(ErrorStateHintConstructorsAbstract value) {
        value.use(); // Expect: State Refinement Error
    }
}

@StateSet({"fresh", "ready"})
class ErrorStateHintConstructorsPrivate {

    @StateRefinement(to = "ready(this)")
    private ErrorStateHintConstructorsPrivate() {}

    @StateRefinement(from = "ready(this)", to = "fresh(this)")
    public void use() {}
}

class ErrorStateHintConstructorsPrivateUser {
    static void fail(ErrorStateHintConstructorsPrivate value) {
        value.use(); // Expect: State Refinement Error
    }
}
