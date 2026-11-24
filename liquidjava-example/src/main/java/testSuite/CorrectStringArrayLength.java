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

    static void sized(String[] names, @Refinement("_ == length(names)") int n) {}

    static void rows(String[][] grid, @Refinement("_ == length(grid)") int n) {}

    public static void initializers() {
        sized(new String[] { "a", "b" }, 2);
        String[] none = {};
        sized(none, 0);
    }

    public static void dimensions() {
        rows(new String[2][3], 2);
    }
}
