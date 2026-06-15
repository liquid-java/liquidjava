package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// SOUNDNESS HOLE: typestate tracking is not robust to aliasing. `b = a` makes b and a the
// same object; after a.close() the object is closed, but the state transition is recorded only on
// variable a, so the verifier still believes b is open and ACCEPTS b.read(). Should be rejected.
@StateSet({ "open", "closed" })
public class ErrorTypestateAliasUnsound {
    private boolean openFlag;

    @StateRefinement(to = "open(this)")
    public ErrorTypestateAliasUnsound() {
        openFlag = true;
    }

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {
        openFlag = false;
    }

    @StateRefinement(from = "open(this)", to = "open(this)")
    public void read() {
        // the "open" precondition, checked at runtime: aborts under -ea when called on a closed object
        assert openFlag : "read() called while closed";
    }

    public static void main(String[] args) {
        ErrorTypestateAliasUnsound a = new ErrorTypestateAliasUnsound();
        ErrorTypestateAliasUnsound b = a;
        a.close();
        b.read(); // State Refinement Error
    }
}
