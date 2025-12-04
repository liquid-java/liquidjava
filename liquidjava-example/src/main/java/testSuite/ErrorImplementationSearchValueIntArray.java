// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;

public class ErrorImplementationSearchValueIntArray {

    @RefinementPredicate("ghost int length(int[])")
    @Refinement("(_ >= -1) && (_ < length(l))")
    public static int getIndexWithValue(
            @Refinement("length(l) > 0") int[] l, @Refinement("i >= 0 && i < length(l)") int i, int val) {
        if (l[i] == val) return i;
        if (i >= l.length) // with or without -1
        return -1;
        else return getIndexWithValue(l, i + 1, val);
    }
}
