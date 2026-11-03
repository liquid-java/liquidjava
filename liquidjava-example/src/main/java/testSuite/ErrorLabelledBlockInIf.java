package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A block left by a labelled break completes normally: the state it leaves reaches the code after the if
@StateSet({"open", "closed"})
public class ErrorLabelledBlockInIf {
    @StateRefinement(to = "open(this)")
    ErrorLabelledBlockInIf() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    void close() {}

    @StateRefinement(from = "open(this)")
    void read() {}

    static void closedInLabelledBlock(boolean c) {
        ErrorLabelledBlockInIf b = new ErrorLabelledBlockInIf();
        if (c) {
            done: {
                b.close();
                break done;
            }
        }
        b.read(); // Expect: State Refinement Error
    }
}
