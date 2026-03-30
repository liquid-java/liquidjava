package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorFunctionInvocation {
    @Refinement(" _ >= a")
    public static int posMult(@Refinement("a == 10") int a, @Refinement("_ < a && _ > 0") int b) {
        @Refinement("y > 30")
        int y = 50;
        return y - 10;
    }

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

    public static void invocation1() {
        @Refinement("_ > 10")
        int p = 10; // Refinement Error
        p = posMult(10, 4);
    }

    public static void invocation2() {
        @Refinement("_ < 1")
        int b = getZero();

        @Refinement("_ > 0")
        int c = getOne();
        c = getZero(); // Refinement Error
    }

    public static void invocationWParams() {
        @Refinement("_ >= 0")
        int p = 10;
        p = posMult(10, 12); // Refinement Error
    }
}
