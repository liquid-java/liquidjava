package testSuite.classes.state_hints_generic_inherited_method_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "nonEmpty"})
@ExternalRefinementsFor("java.util.Stack")
public interface StackRefinements<E> {
    @StateRefinement(to = "empty(this)")
    void Stack();

    // Stack inherits add from Vector, and the verifier does not find this refinement for a call to it
    @StateRefinement(to = "nonEmpty(this)")
    boolean add(E e);

    @StateRefinement(from = "nonEmpty(this)")
    E pop();
}
