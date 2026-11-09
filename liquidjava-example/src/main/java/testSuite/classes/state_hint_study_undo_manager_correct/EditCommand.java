package testSuite.classes.state_hint_study_undo_manager_correct;

import javax.swing.undo.UndoManager;

// Study case A1_56 (JabRef EditCommand) with both calls checked, as the hints say
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
            case REDO -> {
                if (undoManager.canRedo())
                    undoManager.redo();
            }
            case UNDO -> {
                if (undoManager.canUndo())
                    undoManager.undo();
            }
            default -> { }
        }
    }

    // When canUndo() returns false its spec keeps the undo set by name, so the manager is still notUndoable
    static void undoAfterAsking(javax.swing.undo.UndoableEdit edit) {
        UndoManager undoManager = new UndoManager();
        if (!undoManager.canUndo())
            undoManager.addEdit(edit);
        undoManager.undo();
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
