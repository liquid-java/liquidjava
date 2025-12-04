// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorAfterIf2 {
    public static void main(String[] args) {
        @Refinement("k > 0 && k < 100")
        int k = 5;
        if (k > 7) {
            k = 9;
        }
        k = 50;
        @Refinement("_ < 10")
        int m = k;
    }
}
