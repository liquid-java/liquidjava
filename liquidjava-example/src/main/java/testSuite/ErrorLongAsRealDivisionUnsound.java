package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: Java `long` is modeled as a Z3 Real, so `/` becomes exact rational
// division instead of truncating integer division. 7L / 2L == 3 in Java, but the verifier
// models it as 3.5 and ACCEPTS "_ > 3". At runtime 3 > 3 is false. Should be rejected.
@SuppressWarnings("unused")
public class ErrorLongAsRealDivisionUnsound {
    public static void main(String[] args) {
        @Refinement("c == 7")
        long c = 7L;
        @Refinement("d == 2")
        long d = 2L;
        @Refinement("_ > 3")
        long e = c / d; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because e == 3
        assert e > 3 : "e=" + e;
    }
}
