package testSuite;

import liquidjava.specification.Refinement;

public interface ErrorInterfaceStaticReturn {
    @Refinement("_ > 0")
    static int positive() {
        return -1; // Expect: Refinement Error
    }
}
