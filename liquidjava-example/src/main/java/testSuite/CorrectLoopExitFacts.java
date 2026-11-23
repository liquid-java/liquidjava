package testSuite;

import liquidjava.specification.Refinement;

// After a loop that only exits through its condition, the invariants hold and the condition is false
@SuppressWarnings("unused")
public class CorrectLoopExitFacts {
    static boolean next() {
        return true;
    }

    static void atLeast(int i, @Refinement("_ <= i") int n) {}

    static void ten(@Refinement("_ == 10") int x) {}

    static void countUp(int n) {
        int i = 0;
        while (i < n) {
            i++;
        }
        atLeast(i, n);
    }

    static void countUpWithContinue() {
        int i = 0;
        for (; i < 10; i++) {
            if (next())
                continue;
        }
        ten(i);
    }
}
