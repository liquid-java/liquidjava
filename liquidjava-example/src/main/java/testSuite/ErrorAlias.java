// Refinement Error
package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@SuppressWarnings("unused")
@RefinementAlias("InRange(int val, int low, int up) {low < val && val < up}")
public class ErrorAlias {

    @Refinement("InRange( _, 10, 16)")
    public static int getNum() {
        return 14;
    }

    public static void main(String[] args) {
        @Refinement("InRange( _, 10, 15)")
        int j = getNum();
    }
}
