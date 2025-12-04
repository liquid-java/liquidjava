// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorLenZeroIntArray {

    public static int getIndexWithVal(
            @Refinement("length(l) > 0") int[] l, @Refinement("i >= 0 && i <= length(l)") int i, int val) {
        if (l[i] == val) return i;
        if (i >= l.length) return -1;
        else return getIndexWithVal(l, i + 1, val);
    }

    public static void main(String[] args) {
        int[] a = new int[0];
        getIndexWithVal(a, 0, 6);
    }
}
