package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;

@Ghost("int size")
class CounterexampleBag {
    @StateRefinement(to = "size(this) == 0")
    CounterexampleBag() {}
}

public class ErrorCounterexampleGhostArgument {

    static void take(@Refinement("size(b) > 0") CounterexampleBag b) {}

    static void probe(int port, CounterexampleBag bag) {
        if (port < 0)
            return;
        take(bag); // Expect: Refinement Error
    }
}
