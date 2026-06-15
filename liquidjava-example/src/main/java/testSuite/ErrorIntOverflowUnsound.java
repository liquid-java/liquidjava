package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: Java `int` is modeled as an unbounded Z3 integer, so 32-bit
// two's-complement overflow is not modeled. 46341 * 46341 == 2147488281 mathematically,
// but in Java `int` it wraps to -2147479015. The verifier currently ACCEPTS "_ > 0";
// at runtime the value is negative, so the refinement is violated. Should be rejected.
@SuppressWarnings("unused")
public class ErrorIntOverflowUnsound {
    public static void main(String[] args) {
        @Refinement("_ > 0")
        int c = 46341 * 46341; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because c == -2147479015
        assert c > 0 : "c=" + c;
    }
}
