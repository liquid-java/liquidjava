package testSuite;

import liquidjava.specification.Refinement;

public class CorrectEarlyReturn {

    public static int divide(int a, @Refinement("b != 0") int b) {
        return a / b;
    }

    public static void divideUnlessZero(int x, int y) {
        if (y == 0) return;
        divide(x, y);
    }
}
