package testSuite.classes.state_hints_superclass_spec_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"displayable", "notDisplayable"})
@ExternalRefinementsFor("java.awt.Window")
public interface WindowRefinements {
    @StateRefinement(to = "notDisplayable(this)")
    void dispose();

    @StateRefinement(to = "displayable(this)")
    void pack();
}

// the state set is declared in the superclass's spec
@ExternalRefinementsFor("java.awt.Frame")
interface FrameRefinements {
    @StateRefinement(to = "notDisplayable(this)")
    void Frame(String title);

    @StateRefinement(from = "notDisplayable(this)")
    void setUndecorated(boolean undecorated);
}
