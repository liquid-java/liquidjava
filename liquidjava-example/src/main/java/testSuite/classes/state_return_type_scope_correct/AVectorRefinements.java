package testSuite.classes.state_return_type_scope_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Checked before BUndoManagerRefinements: its return type (E) must not become the type of `return` there
@StateSet({"empty", "nonEmpty"})
@ExternalRefinementsFor("java.util.Vector")
public interface AVectorRefinements<E> {
    @StateRefinement(to = "empty(this)")
    public void Vector();

    @StateRefinement(to = "nonEmpty(this)")
    public E set(int index, E element);
}
