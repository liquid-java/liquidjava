package testSuite;

import liquidjava.specification.Refinement;

// The error stops the check of the body before total <= 0 is found not kept by an iteration, but i >= 0 is kept
@SuppressWarnings("unused")
public class ErrorLoopUnprovedInvariant {
    static int read() {
        return 1;
    }

    static void nonNeg(@Refinement("_ >= 0") int x) {}

    static void accumulate() {
        int total = 0;
        for (int i = 0; i < 100; i++) {
            nonNeg(i);
            total = total + read();
            nonNeg(total); // Expect: Refinement Error
        }
    }
}
