package testSuite;

import liquidjava.specification.Refinement;

public record CorrectRecord(int x, int y) {

    static int positive(@Refinement("_ > 0") int v) {
        return v;
    }

    int sum() {
        return positive(1) + x + y;
    }
}
