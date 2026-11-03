package testSuite.classes.state_hint_checked_copy_error;

import java.util.Scanner;

// The failed check is on it and the call on a variable holding the same scanner, so the state was checked
public class Reader {

    public static void nextOfTheSameScanner(Scanner it) {
        Scanner same = it;
        if (!it.hasNext()) {
            same.next(); // Expect: State Refinement Error
        }
    }
}
