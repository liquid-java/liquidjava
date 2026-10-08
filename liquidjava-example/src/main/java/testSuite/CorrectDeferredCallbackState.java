package testSuite;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import liquidjava.specification.Refinement;
import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@ExternalRefinementsFor("javax.swing.undo.UndoManager")
@StateSet({"empty", "full"})
interface CorrectDeferredUndoManagerSpec {
    @StateRefinement(to = "empty(this)") void UndoManager();
    @StateRefinement(to = "full(this)") boolean addEdit(UndoableEdit edit);
    @StateRefinement(from = "full(this)") void undo();
    @StateRefinement(from = "empty(this)") void discardAllEdits();
}

class CorrectDeferredCallbackState {
    UndoManager manager = new UndoManager();
    Runnable listener = () -> { manager.addEdit(null); };

    void fieldCallbackMayNeverRun() {
        manager.discardAllEdits();
    }

    void localCallbackMayNeverRun() {
        UndoManager local = new UndoManager();
        Runnable later = () -> { local.addEdit(null); };
        local.discardAllEdits();
    }

    void directTransitionStillWorks() {
        UndoManager local = new UndoManager();
        local.addEdit(null);
        local.undo();
    }

    void nestedCallbackMayNeverRun() {
        Runnable outer = () -> {
            UndoManager local = new UndoManager();
            Runnable inner = () -> { local.addEdit(null); };
            local.discardAllEdits();
        };
    }

    void callbackInTry() {
        UndoManager local = new UndoManager();
        try {
            Runnable later = () -> { local.addEdit(null); };
            local.discardAllEdits();
        } catch (RuntimeException e) {
            local.discardAllEdits();
        } finally {
            local.discardAllEdits();
        }
    }

    void tryInsideCallback() {
        Runnable later = () -> {
            UndoManager local = new UndoManager();
            try {
                local.addEdit(null);
            } finally {
                local.addEdit(null);
            }
            local.undo();
        };
    }

    void capturedPrimitive(int n) {
        if (n > 0) {
            Runnable later = () -> { manager.addEdit(null); };
            @Refinement("_ > 0") int positive = n;
        }
    }

    void immutableCaptureGuard(int n, boolean choose) {
        if (n > 0) {
            Runnable later = () -> {
                @Refinement("_ > 0") int positive = n;
                if (choose) {
                    @Refinement("_ > 0") int thenPositive = n;
                } else {
                    @Refinement("_ > 0") int elsePositive = n;
                }
            };
            @Refinement("_ > 0") int stillPositive = n;
        }
    }

    void blankPrimitiveCaptureGuard(int input) {
        int n;
        n = input;
        if (n > 0) {
            Runnable later = () -> {
                @Refinement("_ > 0") int positive = n;
            };
            @Refinement("_ > 0") int stillPositive = n;
        }
    }

}
