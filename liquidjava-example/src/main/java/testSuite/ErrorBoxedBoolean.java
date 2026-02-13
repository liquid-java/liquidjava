// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBoxedBoolean {
    public static void main(String[] args) {
        @Refinement("_ == true")
        Boolean b = false;
    }
}
