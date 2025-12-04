// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorAfterIf {
    public void have2(int a, int b) {
        @Refinement("pos > 0")
        int pos = 10;

        if (a > 0 && b > 0) {
            pos = a;
        } else {
            if (b > 0) pos = b;
        }
        @Refinement("_ == a || _ == b")
        int r = pos;
    }
}
