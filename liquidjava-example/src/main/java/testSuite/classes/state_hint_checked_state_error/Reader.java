package testSuite.classes.state_hint_checked_state_error;

import java.util.Scanner;

// The state was checked and the check failed: the state is not unknown, but the check does not decide it either
public class Reader {

    public static void nextWhenThereIsNone(Scanner it) {
        if (!it.hasNext()) {
            it.next(); // Expect: State Refinement Error
        }
    }
}
