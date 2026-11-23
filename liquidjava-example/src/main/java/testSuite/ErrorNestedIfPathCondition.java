package testSuite;

import liquidjava.specification.Refinement;

// What an if nested in a branch establishes holds only on that branch's paths
@SuppressWarnings("unused")
public class ErrorNestedIfPathCondition {
    static void nonPositive(@Refinement("_ <= 0") int x) {}

    static void otherBranch(boolean c, int t) {
        if (c) {
            if (t > 0)
                return;
        } else {
            nonPositive(t); // Expect: Refinement Error
        }
    }

    static void afterBothBranches(boolean c, int t) {
        if (c) {
            if (t > 0)
                return;
        }
        nonPositive(t); // Expect: Refinement Error
    }
}
