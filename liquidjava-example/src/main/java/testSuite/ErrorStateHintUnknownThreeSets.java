package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Only the state set that redo() checks is named, not its combinations with the other two
@StateSet({"start", "canEdit", "end"})
@StateSet({"undoable", "notUndoable"})
@StateSet({"redoable", "notRedoable"})
public class ErrorStateHintUnknownThreeSets {

    @StateRefinement(to = "start(this) && notUndoable(this) && notRedoable(this)")
    public ErrorStateHintUnknownThreeSets() {}

    @StateRefinement(from = "start(this)", to = "canEdit(this)")
    public void addEdit() {}

    @StateRefinement(from = "redoable(this)", to = "undoable(this) && notRedoable(this)")
    public void redo() {}

    @StateRefinement(from = "undoable(this)", to = "notUndoable(this) && redoable(this)")
    public void undo() {}

    public static void redoLast(ErrorStateHintUnknownThreeSets history) {
        history.redo(); // Expect: State Refinement Error
    }
}
