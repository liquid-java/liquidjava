package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectStaticFinalInPredicate {

    // Constants resolved inside the predicate string itself.
    static void clampInt(@Refinement("_ >= Integer.MIN_VALUE && _ <= Integer.MAX_VALUE") int x) {
    }

    static void belowMaxLong(@Refinement("_ <= Long.MAX_VALUE") long x) {
    }

    static void notMaxByte(@Refinement("_ != Byte.MAX_VALUE") int x) {
    }

    public static void main(String[] args) {
        clampInt(0);
        clampInt(Integer.MAX_VALUE);
        clampInt(Integer.MIN_VALUE);
        belowMaxLong(123L);
        notMaxByte(0);
    }
}
