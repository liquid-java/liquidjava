package testSuite.classes.state_hints_redeclared_method_error;

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

    // Frame redeclares addNotify, so on a Frame the verifier does not apply this refinement
    @StateRefinement(to = "displayable(this)")
    void addNotify();
}

@ExternalRefinementsFor("java.awt.Frame")
interface FrameRefinements {
    @StateRefinement(to = "notDisplayable(this)")
    void Frame(String title);

    @StateRefinement(from = "notDisplayable(this)")
    void setUndecorated(boolean undecorated);
}
