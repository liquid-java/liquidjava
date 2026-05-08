package testSuite.classes.state_test_method_correct;

import javax.swing.undo.AbstractUndoableEdit;

public class EditCycler {
    public static void undoIfPossible(AbstractUndoableEdit edit) {
        if (edit.canUndo()) {
            edit.undo();
        }
    }

    public static void redoIfPossible(AbstractUndoableEdit edit) {
        if (edit.canRedo()) {
            edit.redo();
        }
    }

    public static void redoIfPossible() {
        AbstractUndoableEdit edit = new AbstractUndoableEdit();
        edit.canRedo();
        edit.undo();
    }
}
