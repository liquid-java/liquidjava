package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// a ghost named like a state of another class is not shown as that state
@Ghost("boolean ready")
public class ErrorStateValueGhostName {

    @StateRefinement(from = "ready(this)")
    void use() {}

    static void test() {
        ErrorStateValueGhostName sensor = new ErrorStateValueGhostName();
        sensor.use(); // Expect: State Refinement Error
    }
}

@StateSet(name = "phase", value = {"ready", "busy"})
class ErrorStateValueGhostNameMachine {}
