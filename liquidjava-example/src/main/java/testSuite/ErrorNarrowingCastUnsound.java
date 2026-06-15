package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: numeric narrowing casts are not modeled; the (int) cast of 1.9 is ignored
// and the refinement is accepted vacuously. At runtime (int) 1.9 == 1, which does not satisfy
// "_ == 1.9", yet the verifier currently ACCEPTS it. Should be rejected.
@SuppressWarnings("unused")
public class ErrorNarrowingCastUnsound {
    public static void main(String[] args) {
        @Refinement("_ == 1.9")
        int x = (int) 1.9; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because x == 1
        assert x == 1.9 : "x=" + x;
    }
}
