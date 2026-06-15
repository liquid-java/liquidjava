package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectIfThen {

    // a - b overflows when b is very negative (a - b exceeds Integer.MAX_VALUE). Requiring b >= 0 keeps
    // a - b in [1, a] (both operands non-negative with a > b), so it stays positive without overflowing.
    public void have2(int a, @Refinement("b >= 0") int b) {
        @Refinement("pos > 0")
        int pos = 10;
        if (a > 0) {
            if (a > b) pos = a - b;
        }
    }

    public void have1(int a) {
        @Refinement("pos > 0")
        int pos = 10;
        if (a > 0) {
            pos = 5;
            pos = 8;
            pos = 30;
        }
        @Refinement("_ == 30 || _ == 10")
        int u = pos;
    }

    public void haveAnd(int a, @Refinement("b > 5")int b) {
        @Refinement("pos > 0")
        int pos = 10;
        if (a > 0 && b > 0) {
            if (a > b) pos = a - b;
        }
    }

    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;

        if (a > 0) {
            @Refinement("b > 0")
            int b = a;
            b++;
            if (b > 10) {
                @Refinement("_ > 0")
                int c = a;
                @Refinement("_ > 11")
                int d = b + 1;
            }
            if (a > b) {
                @Refinement("_ > b")
                int c = a;
            }
        }
    }
}
