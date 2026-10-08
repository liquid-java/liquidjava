package testSuite.classes.inherited_external_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"displayable", "notDisplayable"})
@ExternalRefinementsFor("javax.swing.JFrame")
public interface JFrameRefinements {
    @StateRefinement(to = "notDisplayable(this)")
    void JFrame(String title);

    @StateRefinement(to = "displayable(this)")
    void pack();

    @StateRefinement(from = "notDisplayable(this)")
    void setUndecorated(boolean undecorated);
}
