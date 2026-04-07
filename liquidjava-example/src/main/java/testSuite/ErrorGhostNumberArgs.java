package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;

public class ErrorGhostNumberArgs {

    @RefinementPredicate("boolean open(int)")
    @Refinement("open(1,2) == true")
    public int one() {
        return 1; // Argument Mismatch Error
    }
}
