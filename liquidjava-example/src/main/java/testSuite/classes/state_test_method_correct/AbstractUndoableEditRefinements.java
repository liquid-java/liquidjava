package testSuite.classes.state_test_method_correct;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"aliveDone", "aliveNotDone", "notAlive"})
@ExternalRefinementsFor("javax.swing.undo.AbstractUndoableEdit")
public interface AbstractUndoableEditRefinements {

    @StateRefinement(to = "aliveDone(this)")
    void AbstractUndoableEdit();

    @StateRefinement(from = "aliveNotDone(this)", to = "aliveDone(this)")
    void redo();

    @StateRefinement(from = "aliveDone(this)", to = "aliveNotDone(this)")
    void undo();

    @StateRefinement(from = "!notAlive(this)", to = "notAlive(this)")
    void die();

    @Refinement("_ == true --> aliveDone(this)")
    boolean canUndo();

    @Refinement("_ == true --> aliveNotDone(this)")
    boolean canRedo();
}
