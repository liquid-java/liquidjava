// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorNullableFieldNullAssumption {
    Integer x;

    void test() {
        mustBeNull(x); // we dont know if x is null or not
    }

    void mustBeNull(@Refinement("_ == null") Integer i) {}
}
