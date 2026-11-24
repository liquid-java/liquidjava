package testSuite;

import liquidjava.specification.Refinement;

public class ErrorTernaryExpression {
    @Refinement(" _ == 3")
    public static int three() {
        return 3;
    }

    public static void main(String[] args) {
        @Refinement("_ < 10")
        int a = 5;
        a = (a == 2) ? 6 + three() : 4 * three(); // Expect: Refinement Error
    }

    static void oneElement(@Refinement("length(a) == 1") int[] a) {}

    static void pick(boolean b) {
        oneElement(b ? new int[1] : new int[2]); // Expect: Refinement Error
    }

    boolean flag;

    void clear() {
        @Refinement("_ == true")
        boolean was = flag ? (flag = false) : true; // Expect: Refinement Error
    }
}
