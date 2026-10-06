package testSuite;

import liquidjava.specification.Refinement;

class CorrectOuterFieldAfterNestedClass {
    @Refinement("_ > 0") int x = 1;

    static class Inner { int y; }

    @Refinement("_ > 0")
    public int get() { return x; }
}
