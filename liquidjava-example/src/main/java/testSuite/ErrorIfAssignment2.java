// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorIfAssignment2 {
    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;
        if (a < 0) a = 100;
    }
}
