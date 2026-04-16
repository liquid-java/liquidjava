package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;

@RefinementPredicate("boolean open(int)")
public class ErrorGhostArgsTypes {
    @Refinement("open(4.5) == true")
    public int one() {
        return 1; // Argument Mismatch Error
    }
}
