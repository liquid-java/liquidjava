package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "marked"})
public class ErrorLoopPostState {
    @StateRefinement(to = "open(this)")
    ErrorLoopPostState() {}

    @StateRefinement(to = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "open(this)")
    void use() {}

    // Issue #338: the loop may run zero times, leaving b open.
    static void whileMayBeEmpty(int n) {
        ErrorLoopPostState b = new ErrorLoopPostState();
        int k = 0;
        while (k < n) {
            b.mark();
            k++;
        }
        b.reset(); // Expect: State Refinement Error
    }

    static void forMayBeEmpty(int n) {
        ErrorLoopPostState b = new ErrorLoopPostState();
        for (int k = 0; k < n; k++) {
            b.mark();
        }
        b.reset(); // Expect: State Refinement Error
    }

    static void forEachMayBeEmpty(int[] values) {
        ErrorLoopPostState b = new ErrorLoopPostState();
        for (int value : values) {
            b.mark();
        }
        b.reset(); // Expect: State Refinement Error
    }

    @StateRefinement(from = "open(this)")
    void explicitThisMayBeEmpty(int n) {
        while (n > 0) {
            this.mark();
            n--;
        }
        this.reset(); // Expect: State Refinement Error
    }

    @StateRefinement(from = "open(this)")
    void implicitThisMayBeEmpty(int n) {
        while (n > 0) {
            mark();
            n--;
        }
        reset(); // Expect: State Refinement Error
    }

    @StateRefinement(from = "open(this)")
    void laterIteration(int n) {
        while (n > 0) {
            use(); // Expect: State Refinement Error
            mark();
            n--;
        }
    }

    @StateRefinement(from = "open(this)")
    void directInvalidCall() {
        this.reset(); // Expect: State Refinement Error
    }

    @StateRefinement(from = "open(this)")
    void directInvalidSequence() {
        mark();
        use(); // Expect: State Refinement Error
    }

    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    void unionDoesNotImplyOneState() {
        use(); // Expect: State Refinement Error
    }
}
