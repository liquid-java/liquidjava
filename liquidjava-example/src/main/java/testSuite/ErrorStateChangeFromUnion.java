package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "marked", "closed"})
public class ErrorStateChangeFromUnion {
    @StateRefinement(to = "open(this)")
    ErrorStateChangeFromUnion() {}

    @StateRefinement(from = "open(this)", to = "marked(this)")
    @StateRefinement(from = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    @StateRefinement(from = "marked(this)")
    void closeIfOpen() {}

    // closed is in no entry state of mark
    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "closed(this)")
    void markFromOpenOrClosed() {
        mark(); // Expect: State Refinement Error
    }

    // after closeIfOpen, the object is closed or still marked, so reset is not allowed
    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    void resetAfterEither() {
        closeIfOpen();
        reset(); // Expect: State Refinement Error
    }
}
