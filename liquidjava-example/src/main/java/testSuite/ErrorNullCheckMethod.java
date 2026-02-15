// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorNullCheckMethod {

    @Refinement("_ != null")
    String returnNotNull(String s) {
        return s; // error: s can be null
    }
}
