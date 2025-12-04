// Argument Mismatch Error
package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@SuppressWarnings("unused")
@RefinementAlias("InRange(int val, int low, int up) {low < val && val < up}")
public class ErrorAliasArgumentSize {

    public static void main(String[] args) {
        @Refinement("InRange(j, 10)")
        int j = 15;
    }
}
