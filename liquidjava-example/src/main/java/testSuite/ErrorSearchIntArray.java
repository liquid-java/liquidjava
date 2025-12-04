// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;

public class ErrorSearchIntArray {

    public static void searchIndex(
            @Refinement("length(l) > 0") int[] l, @Refinement("i >= 0 && i <= length(l)") int i) {
        if (i > l.length) return;
        else searchIndex(l, i + 1);
    }
}
