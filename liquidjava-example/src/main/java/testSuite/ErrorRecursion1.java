// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorRecursion1 {

    @Refinement(" _ == 0")
    public static int untilZero(@Refinement("k >= 0") int k) {
        if (k == 1) return 0;
        else return untilZero(k - 1);
    }
}
