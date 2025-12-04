// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorUnaryOperators {
    public static void main(String[] args) {
        @Refinement("_ < 10")
        int v = 3;
        v--;
        @Refinement("_ >= 10")
        int s = 10;
        s--;
    }
}
