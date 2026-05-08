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

    public static void wrongTester2() { 
        AbstractUndoableEdit edit = new AbstractUndoableEdit();
        edit.undo(); // edit: aliveNotDone
        if (edit.canUndo()) { // is canUndo() == true --> edit: aliveDone
            edit.undo(); // edit: aliveNotDone
        }
        edit.undo(); // is "aliveDone(this)"? not in any path
        
    }
}
