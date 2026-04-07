package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;

@RefinementPredicate("boolean open(int)")
public class ErrorGhostNumberArgs {
    @Refinement("open(1,2) == true")
    public int one() {
        return 1; // Argument Mismatch Error
    }
}
