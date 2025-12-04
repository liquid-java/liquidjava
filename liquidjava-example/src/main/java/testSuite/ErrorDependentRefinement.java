// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorDependentRefinement {
    public static void main(String[] args) {
        @Refinement("_ < 10")
        int smaller = 5;
        @Refinement("bigger > 20")
        int bigger = 50;
        @Refinement("_ > smaller  && _ < bigger")
        int middle = 21;
    }
}
