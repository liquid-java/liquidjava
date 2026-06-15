package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: integer `%` is translated to Z3's Euclidean modulo (result always >= 0),
// but Java `%` takes the sign of the dividend. -7 % 3 == -1 in Java; the verifier models it as 2
// and ACCEPTS "_ >= 0". At runtime -1 >= 0 is false. Should be rejected.
@SuppressWarnings("unused")
public class ErrorNegativeModuloUnsound {
    public static void main(String[] args) {
        @Refinement("a == -7")
        int a = -7;
        @Refinement("_ >= 0")
        int r = a % 3; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because r == -1
        assert r >= 0 : "r=" + r;
    }
}
