package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBitwiseUnknownOperand {
    static void open(@Refinement("_ == 1 || _ == 5") int mode) {
    }

    static void m(int extra) {
        open(1 | extra); // Expect: Refinement Error
    }
}
