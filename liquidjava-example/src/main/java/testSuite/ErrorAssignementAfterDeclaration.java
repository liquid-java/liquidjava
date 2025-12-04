// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorAssignementAfterDeclaration {
    public static void main(String[] args) {
        @Refinement("(z > 0) && (z < 50)")
        int z = 1;
        @Refinement("u < 100")
        int u = 10;
        u = 11 + z;
        u = z * 2;
        u = 30 + z;
        u = 500; // error
    }
}
