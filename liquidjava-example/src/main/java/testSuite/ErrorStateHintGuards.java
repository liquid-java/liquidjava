package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A boolean method whose result decides a state that the failing call needs is suggested as a check, only when the
// refinements prove that checking it allows the call
public class ErrorStateHintGuards {

    // false decides the state: the check is negated
    static void negated() {
        GuardChannel c = GuardChannel.make();
        c.send(); // Expect: State Refinement Error
    }

    // known state with no sequence of calls to the needed state: the check, with its parameters for the caller to fill in
    static void withParameter() {
        GuardLock l = new GuardLock();
        l.unlock(); // Expect: State Refinement Error
    }

    // a true result does not decide the state
    static void undecided() {
        GuardPoll p = GuardPoll.make();
        p.use(); // Expect: State Refinement Error
    }

    // the check needs a state that is just as unknown, but is allowed once the state is known
    static void checkNeedsAState() {
        GuardWriter w = GuardWriter.make();
        w.write(); // Expect: State Refinement Error
    }

    static void checkAllowed() {
        GuardWriter w = new GuardWriter();
        w.write(); // Expect: State Refinement Error
    }

    // the check decides the state set it tests but resets another one the call needs
    static void resetsAnotherSet() {
        GuardBoth b = GuardBoth.make();
        b.both(); // Expect: State Refinement Error
    }

    // the result decides the state only for some arguments
    static void dependsOnArguments() {
        GuardProbe p = GuardProbe.make();
        p.fire(); // Expect: State Refinement Error
    }

    // in some states the refinement applied is not the one that tests the result
    static void otherRefinement() {
        GuardSteps m = GuardSteps.make();
        m.act(); // Expect: State Refinement Error
    }

    // the state changes whatever the result
    static void notACheck() {
        GuardToken t = GuardToken.make();
        t.peek(); // Expect: State Refinement Error
    }

    // a result that is not boolean is not suggested as a check
    static void notBoolean() {
        GuardReader r = new GuardReader();
        r.finish(); // Expect: State Refinement Error
    }

    // several checks would do, so the refinements do not tell which one the call needs
    static void severalChecks() {
        GuardCoarse g = GuardCoarse.make();
        g.run(); // Expect: State Refinement Error
    }
}

@StateSet({"open", "closed"})
class GuardChannel {
    @StateRefinement(to = "open(this)")
    public GuardChannel() {}

    @StateRefinement(to = "return ? closed(this) : open(this)")
    public boolean isClosed() { return false; }

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    @StateRefinement(from = "open(this)")
    public void send() {}

    static GuardChannel make() { return new GuardChannel(); }
}

@StateSet({"unlocked", "locked"})
class GuardLock {
    @StateRefinement(to = "unlocked(this)")
    public GuardLock() {}

    @StateRefinement(from = "unlocked(this)", to = "return ? locked(this) : unlocked(this)")
    public boolean tryLock(long timeout) { return true; }

    @StateRefinement(from = "locked(this)", to = "unlocked(this)")
    public void unlock() {}
}

@StateSet({"ready", "busy"})
class GuardPoll {
    @StateRefinement(to = "busy(this)")
    public GuardPoll() {}

    @StateRefinement(to = "return ? (ready(this) || busy(this)) : busy(this)")
    public boolean poll() { return true; }

    @StateRefinement(from = "ready(this)")
    public void use() {}

    static GuardPoll make() { return new GuardPoll(); }
}

@StateSet({"start", "accept"})
class GuardWriter {
    @StateRefinement(to = "start(this)")
    public GuardWriter() {}

    @StateRefinement(from = "start(this)", to = "return ? accept(this) : start(this)")
    public boolean canWrite() { return true; }

    @StateRefinement(from = "accept(this)")
    public void write() {}

    static GuardWriter make() { return new GuardWriter(); }
}

@StateSet({"undoable", "notUndoable"})
@StateSet({"redoable", "notRedoable"})
class GuardBoth {
    @StateRefinement(to = "notUndoable(this) && notRedoable(this)")
    public GuardBoth() {}

    @StateRefinement(to = "return ? redoable(this) && notUndoable(this) : notRedoable(this)")
    public boolean canRedo() { return true; }

    @StateRefinement(from = "redoable(this) && undoable(this)")
    public void both() {}

    static GuardBoth make() { return new GuardBoth(); }
}

@StateSet({"idle", "armed"})
class GuardProbe {
    @StateRefinement(to = "idle(this)")
    public GuardProbe() {}

    @StateRefinement(to = "(n > 0 && return) ? armed(this) : idle(this)")
    public boolean check(int n) { return true; }

    @StateRefinement(from = "armed(this)")
    public void fire() {}

    static GuardProbe make() { return new GuardProbe(); }
}

@StateSet({"s0", "s1", "s2"})
class GuardSteps {
    @StateRefinement(to = "s0(this)")
    public GuardSteps() {}

    @StateRefinement(from = "s0(this)", to = "return ? s1(this) : s0(this)")
    @StateRefinement(from = "s2(this)", to = "s2(this)")
    public boolean step() { return true; }

    @StateRefinement(from = "s1(this)")
    public void act() {}

    static GuardSteps make() { return new GuardSteps(); }
}

@StateSet({"fresh", "used"})
class GuardToken {
    @StateRefinement(to = "fresh(this)")
    public GuardToken() {}

    @StateRefinement(from = "fresh(this)", to = "used(this)")
    public boolean consume() { return true; }

    @StateRefinement(from = "fresh(this)")
    public void peek() {}

    static GuardToken make() { return new GuardToken(); }
}

@StateSet({"start", "running"})
class GuardCoarse {
    @StateRefinement(to = "start(this)")
    public GuardCoarse() {}

    @StateRefinement(to = "return ? running(this) : true")
    public boolean canRun() { return true; }

    @StateRefinement(to = "return ? running(this) : true")
    public boolean canStop() { return true; }

    @StateRefinement(from = "running(this)")
    public void run() {}

    static GuardCoarse make() { return new GuardCoarse(); }
}

@StateSet({"live", "eof"})
class GuardReader {
    @StateRefinement(to = "live(this)")
    public GuardReader() {}

    @StateRefinement(from = "live(this)", to = "return == -1 ? eof(this) : live(this)")
    public int read() { return 0; }

    @StateRefinement(from = "eof(this)")
    public void finish() {}
}
