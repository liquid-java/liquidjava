package testSuite.classes.state_return_type_scope_correct;

import javax.swing.undo.UndoManager;

public class SimpleTest {
    public static void main(String[] args) {
        UndoManager manager = new UndoManager();
        boolean b = manager.canUndo();
    }
}
