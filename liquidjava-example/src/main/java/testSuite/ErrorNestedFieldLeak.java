package testSuite;

import liquidjava.specification.Refinement;

class ErrorNestedFieldLeak {
    int x = 1;

    static class Inner {
        @Refinement("_ < 0") int x = -1;
    }

    @Refinement("_ < 0")
    public int get() { return x; } // Expect: Refinement Error
}
