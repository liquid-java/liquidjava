package testSuite;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectStaticFinalConstant {

    static void requirePositive(@Refinement("_ > 0") int x) {
    }

    static void requireAtLeast(@Refinement("_ >= 1024") int x) {
    }

    public static void main(String[] args) {
        // Reflective resolution of a JDK static final int constant.
        requirePositive(Integer.MAX_VALUE);

        // Reflective resolution of a JDK static final int with a known concrete value.
        requireAtLeast(Short.MAX_VALUE);
    }

    void other(){
        @Refinement("_ > 0 && _ <= 1") int x = ImageWriteParam.MODE_DEFAULT;
    }
}
