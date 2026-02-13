// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBoxedDouble {
    public static void main(String[] args) {
        @Refinement("_ > 0")
        Double d = -1.0;
    }
}
