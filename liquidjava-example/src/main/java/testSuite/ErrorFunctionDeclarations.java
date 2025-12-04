// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorFunctionDeclarations {
    @Refinement("_ >= d && _ < i")
    private static int range(@Refinement("d >= 0") int d, @Refinement("i > d") int i) {
        return i + 1;
    }
}
