package testSuite;

import liquidjava.specification.Refinement;

public class ErrorStringArrayLength {

    public static void first(@Refinement("length(names) >= 0") String[] names) {
        @Refinement("_ > 0")
        int size = names.length; // Expect: Refinement Error
    }

    static void rows(String[][] grid, @Refinement("_ == length(grid)") int n) {}

    public static void grid() {
        rows(new String[2][3], 3); // Expect: Refinement Error
    }
}
