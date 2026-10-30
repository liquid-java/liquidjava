package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// An object may be in none of the states of a set, before the loop or after a call in its body
@StateSet({"open", "closed"})
public class CorrectLoopStateNone {
    @StateRefinement(to = "!open(this) && !closed(this)")
    CorrectLoopStateNone() {}

    @StateRefinement(from = "!open(this) && !closed(this)", to = "open(this)")
    void open() {}

    @StateRefinement(from = "open(this)", to = "!open(this) && !closed(this)")
    void read() {}

    @StateRefinement(from = "!closed(this)", to = "closed(this)")
    void close() {}

    // every iteration starts and ends in no state
    static void openAndRead(int n) {
        CorrectLoopStateNone value = new CorrectLoopStateNone();
        for (int i = 0; i < n; i++) {
            value.open();
            value.read();
        }
        value.open();
        value.close();
    }
}
