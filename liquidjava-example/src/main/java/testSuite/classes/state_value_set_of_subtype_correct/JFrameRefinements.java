package testSuite.classes.state_value_set_of_subtype_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;

@ExternalRefinementsFor("javax.swing.JFrame")
public interface JFrameRefinements {
    @StateRefinement(to = "display(this) == notDisplayable")
    void JFrame();
}
