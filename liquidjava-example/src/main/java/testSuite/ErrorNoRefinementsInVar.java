// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorNoRefinementsInVar {
    public static void main(String[] args) {
        int a = 11;
        @Refinement("b < 10")
        int b = a;
    }
}
