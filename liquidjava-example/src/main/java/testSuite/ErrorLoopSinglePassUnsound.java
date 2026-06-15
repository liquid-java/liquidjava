package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: loop bodies are modeled as a single iteration (no loop invariant / no
// havoc of mutated variables). The while loop runs until x == 2, so the real post-loop value is
// 2, but the verifier models one pass and ACCEPTS "_ == 1". At runtime y == 1 is false.
// Should be rejected.
@SuppressWarnings("unused")
public class ErrorLoopSinglePassUnsound {
    public static void main(String[] args) {
        int x = 0;
        while (x < 2) {
            x = x + 1;
        }
        @Refinement("_ == 1")
        int y = x; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because y == 2
        assert y == 1 : "y=" + y;
    }
}
