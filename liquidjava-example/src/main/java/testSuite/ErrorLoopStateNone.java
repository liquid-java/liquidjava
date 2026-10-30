package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// An object may be in none of the states of a set, before the loop or after a call in its body
@StateSet({"open", "closed"})
public class ErrorLoopStateNone {
    @StateRefinement(to = "!open(this) && !closed(this)")
    ErrorLoopStateNone() {}

    @StateRefinement(from = "!open(this) && !closed(this)", to = "open(this)")
    void open() {}

    @StateRefinement(from = "open(this)", to = "!open(this) && !closed(this)")
    void read() {}

    // value is in no state before the loop, as after the constructor
    static void noneBeforeLoop(int count) {
        ErrorLoopStateNone value = new ErrorLoopStateNone();
        while (count > 0) {
            value.read(); // Expect: State Refinement Error
            count--;
        }
    }

    // read leaves value in no state, so the second iteration cannot read
    static void noneInSecondIteration(int n) {
        ErrorLoopStateNone value = new ErrorLoopStateNone();
        value.open();
        for (int i = 0; i < n; i++) {
            value.read(); // Expect: State Refinement Error
        }
    }
}
