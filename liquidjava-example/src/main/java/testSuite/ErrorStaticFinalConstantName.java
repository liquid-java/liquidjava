package testSuite;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorStaticFinalConstantName {

    static void setProgressiveMode(@Refinement("_ != ImageWriteParam.MODE_EXPLICIT") int mode) {
    }

    static void direct() {
        setProgressiveMode(ImageWriteParam.MODE_EXPLICIT); // Expect: Refinement Error
    }

    static void throughLocal() {
        int mode = ImageWriteParam.MODE_EXPLICIT;
        setProgressiveMode(mode); // Expect: Refinement Error
    }
}
