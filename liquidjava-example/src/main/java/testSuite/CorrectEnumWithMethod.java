package testSuite;

import liquidjava.specification.Refinement;

enum CorrectEnumWithMethodAction {
    UNDO, REDO;

    String getText() {
        return name();
    }
}

public class CorrectEnumWithMethod {
    static String describe(CorrectEnumWithMethodAction action) {
        @Refinement("_ > 0")
        int width = 1;
        return action.getText() + width;
    }
}
