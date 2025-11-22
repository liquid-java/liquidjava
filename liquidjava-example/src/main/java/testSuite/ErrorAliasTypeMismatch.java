// Argument Mismatch Error
package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;

@SuppressWarnings("unused")
@RefinementAlias("type Positive(int x) { x > 0}")
@RefinementAlias("type PtGrade(double x) { x >= 0 && x <= 20}")
public class ErrorAliasTypeMismatch {

    public static void main(String[] args) {
        @Refinement("PtGrade(_)")
        double positiveGrade2 = 20 * 0.5 + 20 * 0.5;

        @Refinement("Positive(_)")
        double positive = positiveGrade2;
    }
}
