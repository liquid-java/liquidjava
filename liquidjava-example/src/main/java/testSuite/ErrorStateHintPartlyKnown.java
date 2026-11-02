package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Only one of three state sets is known (notRedoable), so the hints name that set only
@StateSet({"start", "canEdit", "end"})
@StateSet({"undoable", "notUndoable"})
@StateSet({"redoable", "notRedoable"})
public class ErrorStateHintPartlyKnown {

    @StateRefinement(to = "start(this) && notUndoable(this) && notRedoable(this)")
    public ErrorStateHintPartlyKnown() {}

    @StateRefinement(from = "redoable(this)", to = "undoable(this) && notRedoable(this)")
    public void redo() {}

    @StateRefinement(from = "undoable(this)", to = "notUndoable(this) && redoable(this)")
    public void undo() {}

    @StateRefinement(from = "notRedoable(this)")
    public void check() {
        this.redo(); // Expect: State Refinement Error
    }
}
