// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorArithmeticFP1 {

    public static void main(String[] args) {
        @Refinement("_ > 5.0")
        double a = 5.0;
    }
}
