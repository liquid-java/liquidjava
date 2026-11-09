package testSuite.classes.state_value_set_of_subtype_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;

// the set is declared on Window, a subclass of Component
@ExternalRefinementsFor("java.awt.Component")
public interface ComponentRefinements {
    @Refinement("_ ? display(this) == displayable : display(this) == notDisplayable")
    boolean isDisplayable();
}
