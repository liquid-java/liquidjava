package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorLongUsage {
    @Refinement(" _ > 40")
    public static long doubleBiggerThanTwenty(@Refinement("a > 20") long a) {
        return a * 2;
    }

    public static void longUsage1() {
        @Refinement("a > 5")
        long a = 9L;

        if (a > 5) {
            @Refinement("b < 50")
            long b = a * 10; // Refinement Error
        }
    }

    public static void longUsage2() {
        @Refinement("a > 5")
        long a = 9L;

        @Refinement("c > 40")
        long c = doubleBiggerThanTwenty(a * 2); // Refinement Error
    }
}
