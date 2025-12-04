// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorSimpleAssignment {
    public static void main(String[] args) {
        @Refinement("c > 2")
        int c = 2; // should emit error
    }
}
