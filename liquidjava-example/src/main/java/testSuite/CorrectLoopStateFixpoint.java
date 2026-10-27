package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The states of an object at the start of every iteration of a loop are those before the loop and those at the end of
// any iteration, computed until they stop changing
@StateSet({"open", "marked", "closed"})
public class CorrectLoopStateFixpoint {
    @StateRefinement(to = "open(this)")
    CorrectLoopStateFixpoint() {}

    @StateRefinement(from = "open(this)")
    @StateRefinement(from = "marked(this)")
    void read() {}

    @StateRefinement(from = "open(this)", to = "marked(this)")
    @StateRefinement(from = "marked(this)")
    void mark() {}

    @StateRefinement(from = "marked(this)", to = "open(this)")
    void reset() {}

    @StateRefinement(from = "!closed(this)", to = "closed(this)")
    void close() {}

    // b is open or marked in every iteration: both allow read and mark
    static void openOrMarked(int n) {
        CorrectLoopStateFixpoint b = new CorrectLoopStateFixpoint();
        for (int i = 0; i < n; i++) {
            b.read();
            b.mark();
        }
        b.read();
        b.close();
    }

    // the body restores the state it starts from
    static void restoredInBody(int n) {
        CorrectLoopStateFixpoint b = new CorrectLoopStateFixpoint();
        while (n > 0) {
            b.mark();
            b.reset();
            n--;
        }
        b.mark();
        b.reset();
    }

    static void nestedLoops(int n) {
        CorrectLoopStateFixpoint b = new CorrectLoopStateFixpoint();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++)
                b.mark();
            b.read();
        }
        b.close();
    }

    static void doWhile(int n) {
        CorrectLoopStateFixpoint b = new CorrectLoopStateFixpoint();
        do {
            b.mark();
            n--;
        } while (n > 0);
        b.reset();
    }

    static void forEach(int[] values) {
        CorrectLoopStateFixpoint b = new CorrectLoopStateFixpoint();
        for (int value : values) {
            b.mark();
            b.read();
        }
        b.read();
    }

    // the only exit is the break, with b marked
    static void exitOnlyThroughBreak(int n) {
        CorrectLoopStateFixpoint b = new CorrectLoopStateFixpoint();
        for (;;) {
            b.mark();
            if (n > 3)
                break;
            b.reset();
            n++;
        }
        b.reset();
    }

    // a continue skips the close, which ends the loop with a return
    static void continueOrClose(int n) {
        CorrectLoopStateFixpoint b = new CorrectLoopStateFixpoint();
        for (int i = 0; i < n; i++) {
            b.read();
            if (i < 5)
                continue;
            b.close();
            return;
        }
        b.close();
    }

    @StateRefinement(from = "open(this)")
    void thisOpenOrMarked(int n) {
        while (n > 0) {
            read();
            this.mark();
            n--;
        }
        read();
    }
}
