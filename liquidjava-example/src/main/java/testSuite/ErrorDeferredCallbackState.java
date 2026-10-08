package testSuite;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;
import liquidjava.specification.Refinement;

@ExternalRefinementsFor("javax.swing.undo.UndoManager")
@StateSet({"empty", "full"})
interface ErrorDeferredUndoManagerSpec {
    @StateRefinement(to = "empty(this)") void UndoManager();
    @StateRefinement(to = "full(this)") boolean addEdit(UndoableEdit edit);
    @StateRefinement(from = "full(this)") void undo();
    @StateRefinement(from = "empty(this)") void discardAllEdits();
}

class ErrorAnonymousCallbackState {
    UndoManager manager = new UndoManager();

    ErrorAnonymousCallbackState() {
        Runnable listener = new Runnable() {
            public void run() { manager.addEdit(null); }
        };
    }

    void undo() {
        manager.undo(); // Expect: State Refinement Error
    }
}

class ErrorLocalLambdaCallbackState {
    void callbackMayNeverRun() {
        UndoManager manager = new UndoManager();
        Runnable listener = () -> { manager.addEdit(null); };
        manager.undo(); // Expect: State Refinement Error
    }
}

class ErrorFieldLambdaCallbackState {
    UndoManager manager = new UndoManager();
    Runnable listener = () -> { manager.addEdit(null); };

    void undo() {
        manager.undo(); // Expect: State Refinement Error
    }
}

class ErrorInvalidLambdaBody {
    void invalidBodyStillChecked() {
        UndoManager manager = new UndoManager();
        Runnable listener = () -> {
            if (true) {
                manager.undo(); // Expect: State Refinement Error
            }
        };
        // The invalid callback must not stop checking its surrounding method.
        manager.undo(); // Expect: State Refinement Error
    }

    void creationStateDoesNotDescribeInvocation() {
        UndoManager manager = new UndoManager();
        manager.addEdit(null);
        Runnable listener = () -> {
            manager.undo(); // Expect: State Refinement Error
        };
    }
}

class ErrorMutableCallbackGuard {
    int value = 1;

    void mutableGuardDoesNotDescribeInvocation() {
        if (value > 0) {
            Runnable later = () -> {
                @Refinement("_ > 0") int positive = value; // Expect: Refinement Error
            };
            @Refinement("_ > 0") int stillPositive = value;
        }
    }
}
