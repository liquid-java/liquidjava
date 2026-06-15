package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: assignments on a not-taken control-flow path are applied to the abstract
// state. In `false && ((x = 1) == 1)` the right operand never executes (short-circuit), so x stays
// 0 at runtime, but the verifier records x = 1 and ACCEPTS "_ == 1". Should be rejected.
@SuppressWarnings("unused")
public class ErrorShortCircuitAssignUnsound {
    public static void main(String[] args) {
        int x = 0;
        boolean b = false && ((x = 1) == 1);
        @Refinement("_ == 1")
        int y = x; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because y == 0
        assert y == 1 : "y=" + y;
    }
}
