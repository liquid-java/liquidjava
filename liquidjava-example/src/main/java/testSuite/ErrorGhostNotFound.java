// Not Found Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorGhostNotFound {

    public void test() {
        @Refinement("notFound(x)")
        int x = 5;
    }
}
