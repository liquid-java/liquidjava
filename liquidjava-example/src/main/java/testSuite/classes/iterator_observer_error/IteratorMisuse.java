package testSuite.classes.iterator_observer_error;

import java.util.Scanner;

public class IteratorMisuse {

    // No check at all: state of the parameter is unknown.
    public static void nextWithoutCheck(Scanner it) {
        it.next(); // State Refinement Error
    }

    // Else branch of hasNext(): condition was false, so we know !hasMore.
    public static void nextInElseBranch(Scanner it) {
        if (it.hasNext()) {
        } else {
            it.next(); // State Refinement Error
        }
    }

    // Negated check: !hasNext() true means hasNext returned false, so !hasMore.
    public static void nextNotInElse(Scanner it) {
        if (!it.hasNext()) {
            it.next(); // State Refinement Error
        }
    }

    // After consuming with next(), state becomes noMore — a second next() in the same
    // then-branch must fail.
    public static void doubleNextInThen(Scanner it) {
        if (it.hasNext()) {
            it.next();
            it.next(); // State Refinement Error
        }
    }

    // Empty if-then: the bug we fixed in visitCtIf would have masked this join, leaking the
    // path-variable's truthiness past the if and silently asserting hasMore.
    public static void nextAfterEmptyIf(Scanner it) {
        if (it.hasNext()) {
        }
        it.next(); // State Refinement Error
    }

    // Sequential ifs: state is consumed by the first then-branch's next(), and the second if's
    // path-variable assertion must not leak past its join.
    public static void sequentialIfsLoseState(Scanner it) {
        if (it.hasNext()) {
            it.next();
        }
        if (it.hasNext()) {
        }
        it.next(); // State Refinement Error
    }

    // Empty if + empty else: neither branch establishes hasMore.
    public static void nextAfterEmptyIfElse(Scanner it) {
        if (it.hasNext()) {
        } else {
        }
        it.next(); // State Refinement Error
    }
}
