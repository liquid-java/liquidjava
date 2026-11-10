package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The class and its superclass each have a set named phase: their states keep the shorthand, since phase == ready
// and phase == busy would read as two values of one set
@StateSet(name = "phase", value = {"ready", "done"})
class ErrorStateSetNameInSuperclassBase {
    @StateRefinement(to = "ready(this)")
    ErrorStateSetNameInSuperclassBase() {}
}

@StateSet(name = "phase", value = {"busy", "idle"})
public class ErrorStateSetNameInSuperclass extends ErrorStateSetNameInSuperclassBase {
    @StateRefinement(to = "ready(this) && idle(this)")
    ErrorStateSetNameInSuperclass() {}

    @StateRefinement(from = "idle(this)", to = "busy(this)")
    void start() {}

    @StateRefinement(from = "ready(this) && busy(this)")
    void use() {}

    public static void main(String[] args) {
        ErrorStateSetNameInSuperclass c = new ErrorStateSetNameInSuperclass();
        c.use(); // Expect: State Refinement Error
    }
}
