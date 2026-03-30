package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class ErrorAfterIf {
    public void afterIf1(int a, int b) {
        @Refinement("pos > 0")
        int pos = 10;

        if (a > 0 && b > 0) {
            pos = a;
        } else {
            if (b > 0)
                pos = b;
        }
        @Refinement("_ == a || _ == b")
        int r = pos; // Refinement Error
    }

    public void afterIf2() {
        @Refinement("k > 0 && k < 100")
        int k = 5;
        if (k > 7) {
            k = 9;
        }
        k = 50;
        @Refinement("_ < 10")
        int m = k; // Refinement Error
    }
}
