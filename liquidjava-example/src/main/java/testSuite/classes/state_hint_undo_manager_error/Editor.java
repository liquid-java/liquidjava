package testSuite.classes.state_hint_undo_manager_error;

import javax.swing.undo.UndoManager;

public class Editor {

    static UndoManager history() {
        return new UndoManager();
    }

    static void redoLast() {
        UndoManager undoManager = history();
        undoManager.redo(); // Expect: State Refinement Error
    }
}
