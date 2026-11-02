package testSuite.classes.found_field_state_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.util.Vector")
@StateSet({"empty", "maybeElements"})
public interface VectorRefinements<E> {

    @StateRefinement(to = "empty(this)")
    public void Vector(@Refinement("_ >= 0") int initialCapacity);

    @StateRefinement(from = "empty(this)", to = "maybeElements(this)")
    @StateRefinement(from = "maybeElements(this)")
    public boolean add(E e);

    @StateRefinement(from = "maybeElements(this)")
    public E get(@Refinement("_ >= 0") int index);
}
