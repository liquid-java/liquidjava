package testSuite.classes.missing_import_final_error;

import javax.imageio.ImageWriteParam;

/** Sibling file that imports the class so the verifier knows where to find it. */
public class ClassImporting {
    public static int explicit() {
        return ImageWriteParam.MODE_EXPLICIT;
    }
}
