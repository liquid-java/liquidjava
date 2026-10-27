package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Typestates through the jumps in a loop body
@StateSet({"open", "marked", "closed"})
public class ErrorLoopStateJumps {
    @StateRefinement(to = "open(this)")
    ErrorLoopStateJumps() {}

    @StateRefinement(from = "open(this)", to = "marked(this)")
    @StateRefinement(from = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "!closed(this)", to = "closed(this)")
    void close() {}

    @StateRefinement(from = "open(this)")
    void needOpen() {}

    // a break out of a labelled block at the end of the body still goes on to the next iteration
    static void labelledBlockEndsBody(int n) {
        ErrorLoopStateJumps b = new ErrorLoopStateJumps();
        while (n > 0) {
            b.close(); // Expect: State Refinement Error
            n--;
            done: {
                break done;
            }
        }
    }

    static void labelledBlockEndsDoWhileBody(int n) {
        ErrorLoopStateJumps b = new ErrorLoopStateJumps();
        do {
            b.close(); // Expect: State Refinement Error
            n--;
            done: {
                break done;
            }
        } while (n > 0);
    }

    // the break leaves the loop and the enclosing labelled block with b marked
    static void breakOutOfEnclosingBlock(int n) {
        ErrorLoopStateJumps b = new ErrorLoopStateJumps();
        out: {
            while (n > 0) {
                b.mark(); // Expect: State Refinement Error
                if (n == 3)
                    break out;
                b.reset();
                n--;
            }
        }
        b.needOpen();
    }

    // the finally closes b after the break, so b is closed after the loop
    static void breakThroughFinally() {
        ErrorLoopStateJumps b = new ErrorLoopStateJumps();
        while (true) {
            try {
                b.mark(); // Expect: State Refinement Error
                break;
            } finally {
                b.close();
            }
        }
        b.reset();
    }
}
