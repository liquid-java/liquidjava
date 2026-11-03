package testSuite.classes.state_hint_undo_manager_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Issue #454: with an unknown state, redo() needs redoable, and the only call that leads there, undo(), needs
// undoable, which is just as unknown
@ExternalRefinementsFor("javax.swing.undo.UndoManager")
@StateSet({"undoable", "notUndoable"})
@StateSet({"redoable", "notRedoable"})
public interface UndoManagerRefinements {

    @StateRefinement(to = "notUndoable(this) && notRedoable(this)")
    void UndoManager();

    @StateRefinement(from = "redoable(this)", to = "undoable(this) && notRedoable(this)")
    void redo();

    @StateRefinement(from = "undoable(this)", to = "notUndoable(this) && redoable(this)")
    void undo();

    @StateRefinement(from = "redoable(this)", to = "return && redoable(this)")
    @StateRefinement(to = "return ? redoable(this) : notRedoable(this)")
    boolean canRedo();
}
