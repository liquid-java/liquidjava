package testSuite.classes.state_hint_checked_error;

import java.util.Scanner;

// A failed check of the receiver does not decide its state; a check of another object says nothing about it
public class Reader {

    public static void failedCheck(Scanner it) {
        if (!it.hasNext()) {
            it.next(); // Expect: State Refinement Error
        }
    }

    public static void failedCheckOfTheSameScanner(Scanner scanner) {
        Scanner same = scanner;
        if (!scanner.hasNext()) {
            same.next(); // Expect: State Refinement Error
        }
    }

    public static void checkOfAnotherScanner(Scanner other, Scanner input) {
        if (other.hasNext()) {
            input.next(); // Expect: State Refinement Error
        }
    }
}
