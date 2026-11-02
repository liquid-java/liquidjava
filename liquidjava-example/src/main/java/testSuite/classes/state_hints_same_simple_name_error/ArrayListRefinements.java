package testSuite.classes.state_hints_same_simple_name_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "nonEmpty"})
@ExternalRefinementsFor("java.util.ArrayList")
public interface ArrayListRefinements<E> {
    @StateRefinement(to = "empty(this)")
    void ArrayList();

    @StateRefinement(to = "nonEmpty(this)")
    boolean add(E e);

    @StateRefinement(from = "nonEmpty(this)")
    E remove(int index);
}
