package testSuite.classes.state_refinement_no_to_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.RefinementPredicate;
import liquidjava.specification.StateRefinement;

@ExternalRefinementsFor("java.util.ArrayList")
@RefinementPredicate("int size(ArrayList l)")
public interface ArrayListRefinements<E> {

    @StateRefinement(to = "size(this) == 0")
    public void ArrayList();

    @StateRefinement(to = "size(this) == size(old(this)) + 1")
    public boolean add(E e);

    @StateRefinement(from = "size(this) > 0")
    public E get(int index);

    @StateRefinement(from = "size(this) == 2", to = "size(this) == size(old(this)) - 1")
    public E remove(int index);
}
