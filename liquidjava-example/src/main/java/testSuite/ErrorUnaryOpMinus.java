// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorUnaryOpMinus {
    public static void main(String[] args) {
        @Refinement("b > 0")
        int b = 8;
        b = -b;
    }
}
