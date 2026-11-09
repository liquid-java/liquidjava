package testSuite.classes.state_set_declared_later_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// uses the states of WindowRefinements, declared after it in this file
@ExternalRefinementsFor("java.awt.Frame")
public interface FrameRefinements {
    @StateRefinement(to = "notDisplayable(this)")
    void Frame();

    @StateRefinement(from = "notDisplayable(this)")
    void setUndecorated(boolean undecorated);
}

@ExternalRefinementsFor("java.awt.Window")
@StateSet({"notDisplayable", "displayable"})
interface WindowRefinements {
    @StateRefinement(to = "displayable(this)")
    void pack();
}
