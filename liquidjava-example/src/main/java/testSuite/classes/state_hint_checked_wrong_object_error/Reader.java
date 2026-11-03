package testSuite.classes.state_hint_checked_wrong_object_error;

import java.util.Scanner;

// The check is on it, but the call is on another scanner, whose state is unknown
public class Reader {

    public static void checkedTheWrongObject(Scanner it, Scanner other) {
        if (it.hasNext()) {
            other.next(); // Expect: State Refinement Error
        }
    }
}
