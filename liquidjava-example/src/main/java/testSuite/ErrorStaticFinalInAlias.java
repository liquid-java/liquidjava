package testSuite;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@SuppressWarnings("unused")
@RefinementAlias("Mode(int x) { x == ImageWriteParam.MODE_DISABLED || x == ImageWriteParam.MODE_DEFAULT "
        + "|| x == ImageWriteParam.MODE_EXPLICIT || x == ImageWriteParam.MODE_COPY_FROM_METADATA }")
public class ErrorStaticFinalInAlias {

    static void setMode(@Refinement("Mode(_)") int mode) {
    }

    public static void main(String[] args) {
        // Modes range from 0 (MODE_DISABLED) to 3 (MODE_COPY_FROM_METADATA).
        setMode(4); // Expect: Refinement Error
    }
}
