package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: exceptional control flow is not modeled. `1 / 0` throws at runtime, so the
// assignment x = 1 after it never executes and x stays 0; but the verifier ignores the throw,
// treats the try body as straight-line code, and ACCEPTS "_ == 1". Should be rejected.
@SuppressWarnings("unused")
public class ErrorExceptionFlowUnsound {
    public static void main(String[] args) {
        int x = 0;
        try {
            int z = 1 / 0;
            x = 1;
        } catch (ArithmeticException ex) {
            x = x;
        }
        @Refinement("_ == 1")
        int y = x; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because y == 0
        assert y == 1 : "y=" + y;
    }
}
