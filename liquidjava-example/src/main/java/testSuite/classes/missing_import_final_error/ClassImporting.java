package testSuite.classes.error_missing_import;

import javax.imageio.ImageWriteParam;

/** Sibling file that imports the class so the verifier knows where to find it. */
public class Helper {
    public static int explicit() {
        return ImageWriteParam.MODE_EXPLICIT;
    }
}
