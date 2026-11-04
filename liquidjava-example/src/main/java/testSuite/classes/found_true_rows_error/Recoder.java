package testSuite.classes.found_true_rows_error;

import java.util.Locale;

import javax.imageio.ImageWriteParam;

// Study case A1_01 (dcm4che SlideRecoder, #375): Found showed the path conditions followed by a bare true row
public class Recoder {

    static void unknownState(ImageWriteParam wParam) {
        wParam.setCompressionType("JPEG"); // Expect: State Refinement Error
    }

    static void unknownStateInBranch(ImageWriteParam wParam, boolean baseline, boolean extended) {
        if (baseline || extended) {
            wParam.setCompressionType("JPEG"); // Expect: State Refinement Error
        }
    }

    static void unknownStateInNestedBranch(ImageWriteParam wParam, boolean lossless, boolean extended) {
        if (lossless) {
            if (!extended) {
                wParam.setCompressionType("JPEG"); // Expect: State Refinement Error
            }
        }
    }

    static void knownStateInBranch(boolean baseline) {
        ImageWriteParam wParam = new ImageWriteParam(Locale.ROOT);
        if (baseline) {
            wParam.setCompressionType("JPEG"); // Expect: State Refinement Error
        }
    }
}
