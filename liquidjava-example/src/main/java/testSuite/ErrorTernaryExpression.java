// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorTernaryExpression {
    @Refinement(" _ == 3")
    public static int three() {
        return 3;
    }

    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;
        a = (a == 2) ? 6 + three() : 4 * three();
    }
}
