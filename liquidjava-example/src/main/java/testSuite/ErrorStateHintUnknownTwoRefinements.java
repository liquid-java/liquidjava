package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Like UndoManager.undoOrRedo(): one state refinement per state set, so the call needs undoable or redoable
@StateSet({"start", "canEdit", "end"})
@StateSet({"undoable", "notUndoable"})
@StateSet({"redoable", "notRedoable"})
public class ErrorStateHintUnknownTwoRefinements {

    @StateRefinement(to = "start(this) && notUndoable(this) && notRedoable(this)")
    public ErrorStateHintUnknownTwoRefinements() {}

    @StateRefinement(from = "undoable(this)", to = "notUndoable(this) && notRedoable(this)")
    @StateRefinement(from = "redoable(this)", to = "notUndoable(this) && notRedoable(this)")
    public void undoOrRedo() {}

    public static void use(ErrorStateHintUnknownTwoRefinements history) {
        history.undoOrRedo(); // Expect: State Refinement Error
    }
}
