package testSuite;

import liquidjava.specification.Refinement;

@SuppressWarnings("unused")
public class CorrectTernaryExpression {
    @Refinement("_ == 3")
    public static int three() {
        return 3;
    }

    static void oneElement(@Refinement("length(a) == 1") int[] a) {}

    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;
        @Refinement("_ > 0")
        int b = 3;
        a = (a == 2) ? 6 : 9;
        a = (b > 2) ? 8 : -1;
        b = (a < 100) ? three() : three() - 1;
        @Refinement("c < 100")
        int c = (a < 100) ? three() : a;
        c = (a < 100) ? three() * 3 : a * 5;

        boolean some = args.length > 0;
        if (some)
            oneElement(some ? new int[1] : new int[2]);
        @Refinement("_ > 0")
        int d = 1;
        d += (some ? a < 100 : false) ? 1 : 1;
    }
}
