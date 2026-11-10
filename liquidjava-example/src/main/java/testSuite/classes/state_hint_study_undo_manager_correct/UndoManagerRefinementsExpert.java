package testSuite.classes.state_hint_study_undo_manager_correct;

import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoableEdit;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// specs-v2: three orthogonal state sets.
// Phase: whether the manager still collects edits (start -> canEdit -> end). It decides addEdit,
// setLimit and discardAllEdits. end() is irreversible: inProgress never becomes true again.
// Undo / Redo: whether undo() / redo() would succeed right now, which is exactly what canUndo() /
// canRedo() answer, in every phase. "notUndoable" means "not known to be undoable": the verifier
// does not count edits, so after an undo() while editing there may or may not be more to undo.
// JDK 21: after end() the manager is one CompoundEdit, and undo/redo alternate over all edits at
// once (AbstractUndoableEdit.hasBeenDone), so end() makes it undoable and not redoable.
@ExternalRefinementsFor("javax.swing.undo.UndoManager")
@StateSet(name="phase", value={"start", "canEdit", "end"})
@StateSet(name="undoState", value={"undoable", "notUndoable"})
@StateSet(name="redoState", value={"redoable", "notRedoable"})
public interface UndoManagerRefinementsExpert {

    @StateRefinement(to="start(this) && notUndoable(this) && notRedoable(this)")
    void UndoManager();

    // A new edit can be undone, and it trims everything that could have been redone.
    @StateRefinement(from="start(this)", to="canEdit(this) && undoable(this) && notRedoable(this)")
    @StateRefinement(from="canEdit(this)", to="canEdit(this) && undoable(this) && notRedoable(this)")
    boolean addEdit(UndoableEdit anEdit);

    @StateRefinement(from="canEdit(this)", to="canEdit(this) && notUndoable(this) && notRedoable(this)")
    void discardAllEdits();

    // end() drops the edits that were undone, and hasBeenDone is true; ending again changes nothing.
    @StateRefinement(from="start(this)", to="end(this) && undoable(this) && notRedoable(this)")
    @StateRefinement(from="canEdit(this)", to="end(this) && undoable(this) && notRedoable(this)")
    @StateRefinement(from="end(this)")
    void end();

    @StateRefinement(from="canEdit(this)")
    @StateRefinement(from="end(this)")
    String getRedoPresentationName();

    @StateRefinement(from="canEdit(this)")
    @StateRefinement(from="end(this)")
    String getUndoOrRedoPresentationName();

    @StateRefinement(from="canEdit(this)")
    @StateRefinement(from="end(this)")
    String getUndoPresentationName();

    // While editing, what was just redone can be undone, and there may be more to redo; after end()
    // it is exact.
    @StateRefinement(from="redoable(this)", to="undoable(this) && notRedoable(this)")
    void redo();

    // A smaller limit trims edits on both sides of the current position (all of them for 0).
    @StateRefinement(from="start(this)")
    @StateRefinement(from="canEdit(this)", to="canEdit(this) && notUndoable(this) && notRedoable(this)")
    void setLimit(int l);

    @StateRefinement(from="undoable(this)", to="notUndoable(this) && redoable(this)")
    void undo();

    @StateRefinement(from="start(this)", to="canEdit(this) && undoable(this) && notRedoable(this)")
    @StateRefinement(from="canEdit(this)", to="canEdit(this) && undoable(this) && notRedoable(this)")
    @StateRefinement(from="end(this)")
    void undoableEditHappened(UndoableEditEvent e);

    // undoOrRedo() undoes at the top of the stack and redoes below it, so either is enough; which one
    // ran is unknown afterwards.
    @StateRefinement(from="undoable(this)", to="notUndoable(this) && notRedoable(this)")
    @StateRefinement(from="redoable(this)", to="notUndoable(this) && notRedoable(this)")
    void undoOrRedo();



    // int getLimit(); //any

    // Queries: they never change the manager, they tell the verifier which state it is in. When it is
    // already known to be undoable / redoable the answer is true; otherwise (not known, or a field whose
    // state is unknown) the answer decides.
    // When canUndo() returns false it keeps the undoState set by name.
    @StateRefinement(from="undoable(this)", to="return && undoable(this)")
    @StateRefinement(to="return ? undoable(this) : undoState(this) == undoState(old(this))")
    boolean canUndo();

    @StateRefinement(from="redoable(this)", to="return && redoable(this)")
    @StateRefinement(to="return ? redoable(this) : notRedoable(this)")
    boolean canRedo();

    // In start nothing was ever added.
    @StateRefinement(from="start(this)", to="!return && start(this)")
    @StateRefinement(from="canEdit(this)")
    @StateRefinement(from="end(this)")
    boolean canUndoOrRedo();

}
