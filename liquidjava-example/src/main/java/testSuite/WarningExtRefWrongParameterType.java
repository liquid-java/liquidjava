package testSuite;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;

@ExternalRefinementsFor("java.util.ArrayList")
@RefinementPredicate("int size(ArrayList l)")
public interface WarningExtRefWrongParameterType<E> {

    @StateRefinement(to = "size(this) == 0")
    public void ArrayList();

    @StateRefinement(to = "size(this) == (size(old(this)) + 1)")
    public boolean add(int wrongParameter);
}
