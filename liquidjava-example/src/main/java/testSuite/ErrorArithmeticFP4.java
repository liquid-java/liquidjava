// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorArithmeticFP4 {
    public static void main(String[] args) {
        @Refinement("_ > 5.0")
        double a = 5.5;

        @Refinement("_ < -5.5")
        double d = -(a - 2.0);
    }
}
