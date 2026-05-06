package testSuite.classes.missing_import_final_error;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ClassNoImport {

    // No import for javax.imageio.ImageWriteParam in this file — the verifier
    // should suggest it because Helper.java already imports it.
    static void requireExplicit(@Refinement("_ == ImageWriteParam.MODE_EXPLICIT") int mode) { // Not Found Error
    }

    public static void main(String[] args) {
        requireExplicit(2);
    }
}
