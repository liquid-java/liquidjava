package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorMissingImportInPredicate {

    // No import for javax.imageio.ImageWriteParam — the verifier should suggest the import.
    static void requireExplicit(@Refinement("_ == ImageWriteParam.MODE_EXPLICIT") int mode) { // Error
    }

    public static void main(String[] args) {
        requireExplicit(2);
    }
}
