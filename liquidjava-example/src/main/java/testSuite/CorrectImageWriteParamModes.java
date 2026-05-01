package testSuite;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectImageWriteParamModes {

    static void requireExplicit(
            @Refinement("_ == ImageWriteParam.MODE_EXPLICIT") int mode) {
    }

    static void requireKnownMode(
            @Refinement("_ == ImageWriteParam.MODE_DISABLED || _ == ImageWriteParam.MODE_DEFAULT "
                    + "|| _ == ImageWriteParam.MODE_EXPLICIT || _ == ImageWriteParam.MODE_COPY_FROM_METADATA") int mode) {
    }

    public static void main(String[] args) {
        requireExplicit(ImageWriteParam.MODE_EXPLICIT);
        requireKnownMode(ImageWriteParam.MODE_DEFAULT);
        requireKnownMode(ImageWriteParam.MODE_DISABLED);
        requireKnownMode(2);
    }
}
