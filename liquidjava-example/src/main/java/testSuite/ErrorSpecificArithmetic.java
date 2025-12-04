// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSpecificArithmetic {
    public static void main(String[] args) {
        @Refinement("_ > 5")
        int a = 10;
        @Refinement("_ > 10")
        int b = a + 1;
        a = 6;
        b = a * 2;
        @Refinement("_ > 20")
        int c = b * -1;
    }
}
