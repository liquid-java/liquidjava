// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSpecificValuesIf2 {
    public static void main(String[] args) {
        @Refinement("_ > 10")
        int a = 15;
        if (a > 14) {
            a = 12;
            @Refinement("_ < 11")
            int c = a;
        }
    }
}
