package testSuite.classes.delegated_subclass_sets_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;

@ExternalRefinementsFor("java.util.Vector")
@Ghost("int elements")
public interface VectorRefinements<E> {
    @StateRefinement(to = "elements(this) == 0")
    void Vector();
}
