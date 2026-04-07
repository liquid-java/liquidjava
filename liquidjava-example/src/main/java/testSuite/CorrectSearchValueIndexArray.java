package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;

@RefinementPredicate("int length(int[])")
public class CorrectSearchValueIndexArray {

    @Refinement("(_ >= -1) && (_ < length(l))")
    public static int getIndexWithValue(
            @Refinement("length(l) > 0") int[] l, @Refinement("i >= 0 && i < length(l)") int i, int val) {
        if (l[i] == val) return i;
        if (i >= l.length - 1) return -1;
        else return getIndexWithValue(l, i + 1, val);
    }

    public static void main(String[] args) {
        int[] arr = new int[10];
        getIndexWithValue(arr, 0, 1000);
    }
}
