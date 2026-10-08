package testSuite;

import liquidjava.specification.Refinement;

public record ErrorRecordMethod(int x, int y) {

    static int positive(@Refinement("_ > 0") int v) {
        return v;
    }

    int bad() {
        return positive(-1); // Expect: Refinement Error
    }
}
