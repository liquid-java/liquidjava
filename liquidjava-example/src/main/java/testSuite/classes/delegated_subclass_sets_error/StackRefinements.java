package testSuite.classes.delegated_subclass_sets_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.util.Stack")
@StateSet({ "empty", "nonEmpty" })
public interface StackRefinements<E> {
    @StateRefinement(from = "empty(this)", to = "nonEmpty(this)")
    E push(E item);

    @StateRefinement(from = "nonEmpty(this)")
    E peek();
}
