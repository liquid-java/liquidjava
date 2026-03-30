package testSuite.classes.index_out_of_bounds_error;

import java.util.ArrayList;

public class Test {
    public static void main(String[] args) {
        ArrayList<Integer> l = new ArrayList<>();
        l.get(0); // Refinement Error
    }
}
