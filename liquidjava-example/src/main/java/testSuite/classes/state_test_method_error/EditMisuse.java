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
        edit.undo();
        if (edit.canUndo()) {
            edit.undo();
        }
        edit.undo(); // State Refinement Error
    }

    // Two undos in the same then-branch: condition forces aliveDone for the first, but the second
    // is in aliveNotDone.
    public static void doubleUndoInThen(AbstractUndoableEdit edit) {
        if (edit.canUndo()) {
            edit.undo();
            edit.undo(); // State Refinement Error
        }
    }

    // Nested ifs: outer canUndo => aliveDone, inner canUndo => aliveDone (still), so calling
    // redo() in the inner then is illegal (redo needs aliveNotDone).
    public static void nestedIfRedoFromAliveDone(AbstractUndoableEdit edit) {
        if (edit.canUndo()) {
            if (edit.canUndo()) {
                edit.redo(); // State Refinement Error
            }
        }
    }

    // Sequential guarded undos with no else: after the first if, state is aliveNotDone in BOTH
    // paths (then took canUndo=>aliveDone then undo=>aliveNotDone; else had canUndo=false meaning
    // !aliveDone, which can only be aliveNotDone given the state set). The second undo therefore
    // cannot succeed — but the verifier must not let the first if's truth assertion leak.
    public static void sequentialIfsLoseState() {
        AbstractUndoableEdit edit = new AbstractUndoableEdit();
        if (edit.canUndo()) {
            edit.undo();
        }
        if (edit.canUndo()) {
        }
        edit.undo(); // State Refinement Error
    }

    // Wrong direction: canRedo() implies aliveNotDone, so calling undo() in that branch is illegal.
    public static void undoGuardedByCanRedo(AbstractUndoableEdit edit) {
        if (edit.canRedo()) {
            edit.undo(); // State Refinement Error
        }
    }

    // Redo after undo inside the then: the inner state is aliveDone -> aliveNotDone -> aliveDone
    // after redo, so a second redo from aliveDone is illegal.
    public static void doubleRedoAfterUndo(AbstractUndoableEdit edit) {
        if (edit.canUndo()) {
            edit.undo();
            edit.redo();
            edit.redo(); // State Refinement Error
        }
    }

    // Empty if / empty else: the bug we fixed would have let `pathVar == true` (or false) leak past
    // the join, masking violations. Here the constructor leaves edit in aliveDone, neither branch
    // changes it, so after the if the state is still aliveDone — calling redo() is illegal.
    public static void redoAfterEmptyIfElse() {
        AbstractUndoableEdit edit = new AbstractUndoableEdit();
        if (edit.canUndo()) {
        } else {
        }
        edit.redo(); // State Refinement Error
    }
}
