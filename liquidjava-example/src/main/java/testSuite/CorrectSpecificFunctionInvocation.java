package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectSpecificFunctionInvocation {
    // a * 2 overflows (wraps negative) once a reaches 2^30, so bound a below 2^30 for the result to stay positive.
    @Refinement(" _ > 0")
    public static int doubleBiggerThanTen(@Refinement("a > 10 && a <= 1073741823") int a) {
        return a * 2;
    }

    public static void main(String[] args) {
        // Upper bound so the call below provably satisfies doubleBiggerThanTen's a <= 1073741823 precondition.
        @Refinement("a > 0 && a <= 1073741823")
        int a = 50;
        int b = doubleBiggerThanTen(a);
    }
}
