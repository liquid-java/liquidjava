package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorStaticFinalInPredicate {

    static void belowMaxByte(@Refinement("_ <= Byte.MAX_VALUE") int x) {
    }

    public static void main(String[] args) {
        // Byte.MAX_VALUE == 127, so 200 violates the bound.
        belowMaxByte(200); // Refinement Error
    }
}
