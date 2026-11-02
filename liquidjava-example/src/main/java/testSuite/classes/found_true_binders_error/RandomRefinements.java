package testSuite.classes.found_true_binders_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;

@ExternalRefinementsFor("java.util.Random")
public interface RandomRefinements {

    public void Random();

    public int nextInt(@Refinement("bound > 0") int bound);
}
