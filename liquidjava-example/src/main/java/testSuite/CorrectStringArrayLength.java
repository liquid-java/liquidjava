package testSuite;

import liquidjava.specification.Refinement;

public class CorrectStringArrayLength {

    static int nonNegative(@Refinement("_ >= 0") int x) {
        return x;
    }

    public static int count(@Refinement("length(names) > 0") String[] names) {
        int n = 0;
        for (int i = 0; i < names.length; i++) {
            n = nonNegative(i - i);
        }
        @Refinement("_ > 0")
        int size = names.length;
        return n + size;
    }
}
