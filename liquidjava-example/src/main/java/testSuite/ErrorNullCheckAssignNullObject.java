// Refinement Error
package testSuite;

import java.util.Date;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorNullCheckAssignNullObject {
    public static void main(String[] args) {
        @Refinement("_ != null")
        Date date = null;
    }
}
