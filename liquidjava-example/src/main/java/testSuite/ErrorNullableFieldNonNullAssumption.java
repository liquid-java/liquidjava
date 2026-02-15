// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorNullableFieldNonNullAssumption {
    Integer x;

    void test() {
        mustBeNonNull(x); // we dont know if x is null or not
    }

    void mustBeNonNull(@Refinement("_ != null") Integer i) {}
}
