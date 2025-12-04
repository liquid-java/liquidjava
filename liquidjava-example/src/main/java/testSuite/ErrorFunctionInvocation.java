// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorFunctionInvocation {
    @Refinement(" _ >= a")
    public static int posMult(@Refinement("a == 10") int a, @Refinement("_ < a && _ > 0") int b) {
        @Refinement("y > 30")
        int y = 50;
        return y - 10;
    }

    public static void main(String[] args) {
        @Refinement("_ > 10")
        int p = 10;
        p = posMult(10, 4);
    }
}
