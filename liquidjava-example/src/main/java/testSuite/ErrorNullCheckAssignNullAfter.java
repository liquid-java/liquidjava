// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorNullCheckAssignNullAfter {
    public static void main(String[] args) {
        @Refinement("_ != null")
        String s = "not null";
        s = null; // error
    }
}
