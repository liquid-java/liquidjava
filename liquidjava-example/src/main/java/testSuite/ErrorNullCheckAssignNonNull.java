// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorNullCheckAssignNonNull {
    public static void main(String[] args) {
        @Refinement("_ == null")
        String s = "not null";
    }
}
