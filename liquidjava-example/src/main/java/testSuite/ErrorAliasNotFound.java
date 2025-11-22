// Not Found Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorAliasNotFound {

    public static void main(String[] args) {
        @Refinement("UndefinedAlias(x)")
        int x = 5;
    }
}
