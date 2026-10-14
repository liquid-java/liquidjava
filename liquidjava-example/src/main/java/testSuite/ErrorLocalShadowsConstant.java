package testSuite;

import liquidjava.specification.Refinement;

// #321: a local named like a constant must not stand for the constant
public class ErrorLocalShadowsConstant {
    enum Encoding {
        JPG, PNG
    }

    static final int LIMIT = 10;

    static void onlyPng(@Refinement("_ == Encoding.PNG") Encoding f) {}

    public static void main(String[] args) {
        Encoding JPG = Encoding.PNG;
        Encoding g = Encoding.JPG;
        onlyPng(g); // Expect: Refinement Error
    }

    static void staticConstant() {
        int LIMIT = 3;
        @Refinement("_ == 3")
        int k = ErrorLocalShadowsConstant.LIMIT; // Expect: Refinement Error
    }
}
