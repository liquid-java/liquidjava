package testSuite;

import liquidjava.specification.Refinement;

// After a loop that may exit elsewhere than at its condition, the condition is not known to be false
@SuppressWarnings("unused")
public class ErrorLoopExitFacts {
    static boolean next() {
        return true;
    }

    static void atLeast(int i, @Refinement("_ <= i") int n) {}

    static void atMostThree(@Refinement("_ <= 3") int x) {}

    static void ten(@Refinement("_ == 10") int x) {}

    static void exitByBreak(int n) {
        int i = 0;
        while (i < n) {
            if (next())
                break;
            i++;
        }
        atLeast(i, n); // Expect: Refinement Error
    }

    // i <= 3 holds at the start of each iteration, but the loop exits after the body, with i == 5
    static void doWhile() {
        int i = 0;
        do {
            i = i + 5;
        } while (i < 3);
        atMostThree(i); // Expect: Refinement Error
    }

    // the condition increments i after it is checked, so the loop exits with i == 11
    static void conditionWithUpdate() {
        int i = 0;
        while (i++ < 10) {
        }
        ten(i); // Expect: Refinement Error
    }
}
