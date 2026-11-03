package testSuite.classes.state_hint_interface_spec_error;

import java.util.concurrent.RunnableFuture;

public class Use {

    static boolean finished(RunnableFuture<String> task) {
        return task.isDone(); // Expect: State Refinement Error
    }
}
