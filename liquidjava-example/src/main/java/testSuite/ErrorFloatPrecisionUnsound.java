package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: `float` is modeled as 64-bit FP, not Java's 32-bit binary32. The float
// literal 0.1f rounds to 0.100000001490116119... at runtime, which is not equal to 0.1, yet the
// verifier ACCEPTS "_ == 0.1". Should be rejected.
@SuppressWarnings("unused")
public class ErrorFloatPrecisionUnsound {
    public static void main(String[] args) {
        @Refinement("_ == 0.1")
        float f = 0.1f; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because (float)0.1 != 0.1
        assert f == 0.1 : "f=" + f;
    }
}
