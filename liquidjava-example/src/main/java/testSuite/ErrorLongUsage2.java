// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorLongUsage2 {

    @Refinement(" _ > 40")
    public static long doubleBiggerThanTwenty(@Refinement("a > 20") long a) {
        return a * 2;
    }

    public static void main(String[] args) {
        @Refinement("a > 5")
        long a = 9L;

        @Refinement("c > 40")
        long c = doubleBiggerThanTwenty(a * 2);
    }
}
