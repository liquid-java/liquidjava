package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "marked"})
public class CorrectLoopPostState {
    @StateRefinement(to = "open(this)")
    CorrectLoopPostState() {}

    @StateRefinement(to = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "open(this)")
    void use() {}

    // A state-preserving call leaves the entry state available after the loop.
    static void unchangedState(int n) {
        CorrectLoopPostState b = new CorrectLoopPostState();
        while (n > 0) {
            b.use();
            n--;
        }
        b.use();
    }

    // An unconditional transition after the loop establishes the required state.
    static void establishStateAfterLoop(int n) {
        CorrectLoopPostState b = new CorrectLoopPostState();
        while (n > 0) {
            b.mark();
            n--;
        }
        b.mark();
        b.reset();
    }

    @StateRefinement(from = "open(this)")
    void unchangedThisState(int n) {
        while (n > 0) {
            this.use();
            n--;
        }
        use();
    }

    @StateRefinement(from = "open(this)")
    void establishThisStateAfterLoop(int n) {
        while (n > 0) {
            mark();
            n--;
        }
        this.mark();
        reset();
    }

    @StateRefinement(from = "open(this)")
    void directValidTransitions() {
        this.use();
        mark();
        this.reset();
        use();
    }

    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    void acceptEitherState() {}

    // Neither alternative is known individually; the call accepts their union and preserves it.
    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    void wrapEitherState() {
        acceptEitherState();
        this.acceptEitherState();
    }
}
