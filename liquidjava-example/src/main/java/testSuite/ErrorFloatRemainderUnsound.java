package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: floating-point `%` is translated to the IEEE-754 remainder (mkFPRem),
// but Java `%` is fmod (truncated remainder). 7.0 % 4.0 == 3.0 in Java; the IEEE remainder is
// -1.0, so the verifier ACCEPTS "_ < 1.0". At runtime 3.0 < 1.0 is false. Should be rejected.
@SuppressWarnings("unused")
public class ErrorFloatRemainderUnsound {
    public static void main(String[] args) {
        @Refinement("_ < 1.0")
        double r = 7.0 % 4.0; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because r == 3.0
        assert r < 1.0 : "r=" + r;
    }
}
