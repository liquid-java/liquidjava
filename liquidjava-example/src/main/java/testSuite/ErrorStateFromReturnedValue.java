package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The state of an object assigned the value of a call comes from the call's return refinement
@StateSet(name = "phase", value = {"idle", "running"})
class FromReturnedTask {
    @StateRefinement(to = "idle(this)")
    FromReturnedTask() {}

    @StateRefinement(from = "idle(this)", to = "running(this)")
    void start() {}

    @StateRefinement(from = "running(this)")
    void await() {}

    @Refinement("idle(_)")
    static FromReturnedTask create() {
        return new FromReturnedTask();
    }
}

public class ErrorStateFromReturnedValue {
    static void use() {
        FromReturnedTask task = FromReturnedTask.create();
        task.await(); // Expect: State Refinement Error
    }
}
