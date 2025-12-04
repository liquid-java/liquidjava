// Not Found Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorGhostNotFound {

    public void test() {
        @Refinement("notFound(x)")
        int x = 5;
    }
}
