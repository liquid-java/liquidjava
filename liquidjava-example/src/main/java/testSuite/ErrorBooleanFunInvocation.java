// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorBooleanFunInvocation {

    @Refinement("_ == (n > 10)")
    public static boolean greaterThanTen(int n) {
        return n > 10;
    }

    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;

        @Refinement("_ == true")
        boolean k = (a < 11);

        @Refinement("_ == true")
        boolean o = !(a == 12);

        @Refinement("_ == true")
        boolean m = greaterThanTen(a);
    }
}
