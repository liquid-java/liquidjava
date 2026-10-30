package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Loops with jumps whose states are not followed (a break to a labelled block, or through a finally) havoc the
// typestates of the objects they change, as before the fixpoint: these tests pin that fallback
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

    // break done: the state of b is unknown in the body (the fixpoint would find it closed in the second iteration)
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

    // break out: the state of b is unknown in the body (the fixpoint would miss the marked state at the break)
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

    // a break through a finally: the state of b is unknown in the body (the finally closes b after the break)
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
