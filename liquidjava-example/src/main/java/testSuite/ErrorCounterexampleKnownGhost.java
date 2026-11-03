package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@Ghost("int size")
class CounterexampleStack {
    @StateRefinement(to = "size(this) == 0")
    CounterexampleStack() {}

    @StateRefinement(to = "size(this) == size(old(this)) + 1")
    void push() {}
}

public class ErrorCounterexampleKnownGhost {

    static void take(@Refinement("size(s) > 1") CounterexampleStack s) {}

    static void probe(int port) {
        CounterexampleStack s = new CounterexampleStack();
        if (port < 0)
            return;
        s.push();
        take(s); // Expect: Refinement Error
    }
}
