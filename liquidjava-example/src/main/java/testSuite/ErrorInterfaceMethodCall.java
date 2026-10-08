package testSuite;

import liquidjava.specification.Refinement;

public interface ErrorInterfaceMethodCall {
    static void requirePositive(@Refinement("_ > 0") int value) {}

    static void badCall() {
        requirePositive(-1); // Expect: Refinement Error
    }
}
