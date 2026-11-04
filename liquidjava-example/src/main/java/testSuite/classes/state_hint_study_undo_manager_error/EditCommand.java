package testSuite.classes.state_hint_study_undo_manager_error;

import javax.swing.undo.UndoManager;

// Study case A1_56 (JabRef EditCommand), trimmed to the calls on the undo manager: it comes from the frame, so its
// state is unknown, and canRedo() and canUndo() are the checks that decide it
public class EditCommand {
    private final JabRefFrame frame;
    private final StandardActions action;

    public EditCommand(StandardActions action, JabRefFrame frame) {
        this.action = action;
        this.frame = frame;
    }

    public void execute() {
        CountingUndoManager undoManager = frame.getUndoManager();
        switch (action) {
            case REDO -> undoManager.redo(); // Expect: State Refinement Error
            case UNDO -> undoManager.undo();
            default -> { }
        }
    }

    // redo() checked as the first hint says: undo() needs the other check
    public void executeRedoChecked() {
        CountingUndoManager undoManager = frame.getUndoManager();
        switch (action) {
            case REDO -> {
                if (undoManager.canRedo())
                    undoManager.redo();
            }
            case UNDO -> undoManager.undo(); // Expect: State Refinement Error
            default -> { }
        }
    }
}

enum StandardActions {
    COPY, UNDO, REDO
}

class CountingUndoManager extends UndoManager {
}

class JabRefFrame {
    private final CountingUndoManager undoManager;

    JabRefFrame(CountingUndoManager undoManager) {
        this.undoManager = undoManager;
    }

    CountingUndoManager getUndoManager() {
        return undoManager;
    }
}
