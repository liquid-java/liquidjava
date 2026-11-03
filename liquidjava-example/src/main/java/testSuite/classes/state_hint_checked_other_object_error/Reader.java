package testSuite.classes.state_hint_checked_other_object_error;

import java.util.Scanner;

// Another scanner was checked, so nothing is known about the state of the receiver
public class Reader {

    public static void nextOfTheOther(Scanner other, Scanner it) {
        if (other.hasNext()) {
            it.next(); // Expect: State Refinement Error
        }
    }
}
