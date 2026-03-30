package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorArithmeticFP {

    private static void arithmetic1() {
        @Refinement("_ > 5.0")
        double a = 5.0; // Refinement Error
    }

    private static void arithmetic2() {
        @Refinement("_ > 5.0")
        double a = 5.5;

        @Refinement("_ == 10.0")
        double c = a * 2.0; // Refinement Error
    }

    private static void arithmetic3() {
        @Refinement("_ > 5.0")
        double a = 5.5;

        @Refinement("_ < -5.5")
        double d = -a; // Refinement Error
    }

    private static void arithmetic4() {
        @Refinement("_ > 5.0")
        double a = 5.5;

        @Refinement("_ < -5.5")
        double d = -(a - 2.0); // Refinement Error
    }
}
