package testSuite;

import liquidjava.specification.Refinement;

// SOUNDNESS HOLE: a field's refinement is assumed on read without proving it was ever
// established. The field n defaults to 0, but the verifier trusts "@Refinement(_ > 0)" on the
// field and ACCEPTS reading it into a "_ > 0" variable. At runtime n == 0. Should be rejected.
@SuppressWarnings("unused")
public class ErrorFieldDefaultUnsound {
    @Refinement("_ > 0")
    int n;

    public static void main(String[] args) {
        ErrorFieldDefaultUnsound o = new ErrorFieldDefaultUnsound();
        @Refinement("_ > 0")
        int x = o.n; // Refinement Error
        // runtime check mirrors the refinement; aborts under -ea because n defaults to 0
        assert x > 0 : "x=" + x;
    }
}
