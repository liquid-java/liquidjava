package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A call whose entry states are each unprovable but whose union is provable is allowed, and the object ends up in a
// state given by any alternative whose entry state it may be in
@StateSet({"open", "marked", "closed"})
public class CorrectStateChangeFromUnion {
    @StateRefinement(to = "open(this)")
    CorrectStateChangeFromUnion() {}

    @StateRefinement(from = "open(this)", to = "marked(this)")
    @StateRefinement(from = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    @StateRefinement(from = "marked(this)", to = "closed(this)")
    void close() {}

    // open goes to marked, marked stays marked
    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    void markFromEither() {
        mark();
        reset();
    }

    // both alternatives go to closed
    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    void closeFromEither() {
        close();
    }
}
