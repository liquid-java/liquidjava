package testSuite;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorImageWriteParamModes {

    static void requireExplicit(@Refinement("_ == ImageWriteParam.MODE_EXPLICIT") int mode) {
    }

    public static void main(String[] args) {
        // MODE_DEFAULT is 1, not 2 (MODE_EXPLICIT).
        requireExplicit(ImageWriteParam.MODE_DEFAULT); // Refinement Error
    }
}
