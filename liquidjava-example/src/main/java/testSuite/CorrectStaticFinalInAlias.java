package testSuite;

import javax.imageio.ImageWriteParam;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@SuppressWarnings("unused")
@RefinementAlias("Mode(int x) { x == ImageWriteParam.MODE_DISABLED || x == ImageWriteParam.MODE_DEFAULT "
        + "|| x == ImageWriteParam.MODE_EXPLICIT || x == ImageWriteParam.MODE_COPY_FROM_METADATA }")
@RefinementAlias("BelowMaxByte(int x) { x <= Byte.MAX_VALUE }")
public class CorrectStaticFinalInAlias {

    static void setMode(@Refinement("Mode(_)") int mode) {
    }

    static void setSmall(@Refinement("BelowMaxByte(_)") int x) {
    }

    public static void main(String[] args) {
        setMode(ImageWriteParam.MODE_EXPLICIT);
        setMode(ImageWriteParam.MODE_DISABLED);
        setMode(3);
        setSmall(127);
    }
}
