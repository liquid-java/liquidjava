package testSuite.classes.state_test_method_error;

import javax.swing.undo.AbstractUndoableEdit;

public class EditMisuse {

    public static void undoInElseBranch(AbstractUndoableEdit edit) {
        if (edit.canUndo()) {
        } else {
            edit.undo(); // State Refinement Error
        }
    }

    public static void undoNotInElse(AbstractUndoableEdit edit) {
        if (!edit.canUndo()) {
            edit.undo(); // State Refinement Error
        } 
    }
    public static void wrongTesterForRedo(AbstractUndoableEdit edit) {
        if (edit.canUndo()) {
            edit.redo(); // State Refinement Error
        }
    }
}
