package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The needed and current states are named only in the state sets that matter
@StateSet({"created", "open", "ready"})
@StateSet({"quiet", "loud"})
public class ErrorStateHintNaming {

    @StateRefinement(to = "created(this) && quiet(this)")
    public ErrorStateHintNaming() {}

    // a state of either set: each is named alone, not combined with every state of the other set
    @StateRefinement(from = "open(this) || loud(this)")
    public void read() {}

    public static void use(ErrorStateHintNaming s) {
        s.read(); // Expect: State Refinement Error
    }
}

// every state allows flip(), each by its own refinement: a or b, not all six states
@StateSet({"a", "b"})
@StateSet({"x", "y", "z"})
class ErrorStateHintNamingToggle {

    @StateRefinement(to = "a(this) && x(this)")
    public ErrorStateHintNamingToggle() {}

    @StateRefinement(from = "a(this)", to = "b(this)")
    @StateRefinement(from = "b(this)", to = "a(this)")
    public void flip() {}

    public static void use(ErrorStateHintNamingToggle t) {
        t.flip(); // Expect: State Refinement Error
    }
}

// known in one of three state sets: only that set is named
@StateSet({"start", "canEdit", "end"})
@StateSet({"undoable", "notUndoable"})
@StateSet({"redoable", "notRedoable"})
class ErrorStateHintNamingPartlyKnown {

    @StateRefinement(to = "start(this) && notUndoable(this) && notRedoable(this)")
    public ErrorStateHintNamingPartlyKnown() {}

    @StateRefinement(from = "redoable(this)", to = "undoable(this) && notRedoable(this)")
    public void redo() {}

    @StateRefinement(from = "undoable(this)", to = "notUndoable(this) && redoable(this)")
    public void undo() {}

    @StateRefinement(from = "notRedoable(this)")
    public void check() {
        this.redo(); // Expect: State Refinement Error
    }
}
