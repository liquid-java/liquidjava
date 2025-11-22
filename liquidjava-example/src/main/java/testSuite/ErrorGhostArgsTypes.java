// Argument Mismatch Error
package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementPredicate;

public class ErrorGhostArgsTypes {
    @RefinementPredicate("ghost boolean open(int)")
    @Refinement("open(4.5) == true")
    public int one() {
        return 1;
    }
}
