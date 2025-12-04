// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBooleanLiteral {
    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;

        @Refinement("_ == true")
        boolean k = (a < 11);

        @Refinement("_ == false")
        boolean t = !(a == 12);
    }
}
