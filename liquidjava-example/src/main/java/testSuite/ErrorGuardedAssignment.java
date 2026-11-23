package testSuite;

import liquidjava.specification.Refinement;

// A guard on a variable must not be applied to the value assigned to it afterwards (#538)
@SuppressWarnings("unused")
public class ErrorGuardedAssignment {
    static int read() {
        return 1;
    }

    static void lessThanFive(@Refinement("_ < 5") int x) {}

    static void negative(@Refinement("_ < 0") int x) {}

    static void assigned() {
        int t = read();
        if (t < 5) {
            t = t + 10;
            lessThanFive(t); // Expect: Refinement Error
        }
    }

    static void incremented() {
        int t = read();
        if (t < 5) {
            t++;
            lessThanFive(t); // Expect: Refinement Error
        }
    }

    // after the if, t is the value assigned in the only branch that continues, which the condition was not checked on
    static void assignedInOnlyBranchThatContinues() {
        int t = read();
        if (t < 5) {
            t = 100;
        } else {
            return;
        }
        negative(1); // Expect: Refinement Error
    }
}
