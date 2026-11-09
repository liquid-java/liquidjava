package testSuite.classes.state_value_set_of_subtype_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("java.awt.Window")
@StateSet(name = "display", value = {"notDisplayable", "displayable"})
public interface WindowRefinements {
    @StateRefinement(from = "display(this) == displayable", to = "display(this) == notDisplayable")
    void dispose();
}
