package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorIfAssignment {
    public static void ifAssignment1() {
        @Refinement("_ < 10")
        int a = 5;

        if (a > 0) {
            @Refinement("b > 0")
            int b = a;
            b++;
            a = 10; // Refinement Error
        }
    }

    public static void ifAssignment2() {
        @Refinement("_ < 10")
        int a = 5;
        if (a < 0)
            a = 100; // Refinement Error
    }
}
