package testSuite;

import liquidjava.specification.Refinement;

public interface ErrorInterfaceDefaultReturn {
    @Refinement("_ > 0")
    default int positive() {
        return -1; // Expect: Refinement Error
    }
}
