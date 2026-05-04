package testSuite;

import static java.lang.Math.PI;

import liquidjava.specification.Refinement;

/**
 * Locks in that the import walk ignores {@code import static} (kind {@code FIELD}) without confusing the resolver:
 * the file has a static import for an unrelated symbol, and a refinement that uses a regular {@code Type.CONST}
 * reference. The verifier must skip the static import and resolve {@code Math.PI} via the implicit {@code java.lang}
 * fallback (or the static-import target's class — either path is correct here).
 */
@SuppressWarnings("unused")
public class CorrectStaticImportInPredicate {

    static double useUnused() {
        return PI;
    }

    static void requireBelowFour(@Refinement("_ < 4.0") double x) {
    }

    public static void main(String[] args) {
        requireBelowFour(Math.PI);
    }
}
