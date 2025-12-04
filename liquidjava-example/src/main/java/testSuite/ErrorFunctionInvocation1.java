// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorFunctionInvocation1 {
    @Refinement("_ == 2")
    private static int getTwo() {
        return 1 + 1;
    }

    @Refinement(" _ == 0")
    private static int getZero() {
        return 0;
    }

    @Refinement("_ == 1")
    private static int getOne() {
        @Refinement("_ == 0")
        int a = getZero();
        return a + 1;
    }

    public static void main(String[] args) {
        @Refinement("_ < 1")
        int b = getZero();

        @Refinement("_ > 0")
        int c = getOne();
        c = getZero();
    }
}
