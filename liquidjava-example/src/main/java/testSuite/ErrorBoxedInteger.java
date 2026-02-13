// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBoxedInteger {
    public static void main(String[] args) {
        @Refinement("_ > 0")
        Integer j = -1;
    }
}
