package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// The programs of ErrorStateHintGuards and ErrorStateHintArgumentConditions with their hints followed
public class CorrectStateHintGuards {

    static void negated() {
        FixedChannel c = FixedChannel.make();
        if (!c.isClosed())
            c.send();
    }

    static void withParameter() {
        FixedLock l = new FixedLock();
        if (l.tryLock(10))
            l.unlock();
    }

    static void checkAllowed() {
        FixedWriter w = new FixedWriter();
        if (w.canWrite())
            w.write();
    }

    static void twoParameters() {
        FixedRange r = new FixedRange();
        r.set(1, 2);
        r.scan();
    }

    static void thenAnotherCall() {
        FixedChain chain = new FixedChain();
        chain.pick(1);
        chain.next();
        chain.done();
    }
}

@StateSet({"open", "closed"})
class FixedChannel {
    @StateRefinement(to = "open(this)")
    public FixedChannel() {}

    @StateRefinement(to = "return ? closed(this) : open(this)")
    public boolean isClosed() { return false; }

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    @StateRefinement(from = "open(this)")
    public void send() {}

    static FixedChannel make() { return new FixedChannel(); }
}

@StateSet({"unlocked", "locked"})
class FixedLock {
    @StateRefinement(to = "unlocked(this)")
    public FixedLock() {}

    @StateRefinement(from = "unlocked(this)", to = "return ? locked(this) : unlocked(this)")
    public boolean tryLock(long timeout) { return true; }

    @StateRefinement(from = "locked(this)", to = "unlocked(this)")
    public void unlock() {}
}

@StateSet({"start", "accept"})
class FixedWriter {
    @StateRefinement(to = "start(this)")
    public FixedWriter() {}

    @StateRefinement(from = "start(this)", to = "return ? accept(this) : start(this)")
    public boolean canWrite() { return true; }

    @StateRefinement(from = "accept(this)")
    public void write() {}

    static FixedWriter make() { return new FixedWriter(); }
}

@StateSet({"low", "high"})
class FixedRange {
    @StateRefinement(to = "low(this)")
    public FixedRange() {}

    @StateRefinement(to = "lo < hi ? high(this) : low(this)")
    public void set(int lo, int hi) {}

    @StateRefinement(from = "high(this)")
    public void scan() {}
}

@StateSet({"a", "b", "c"})
class FixedChain {
    @StateRefinement(to = "a(this)")
    public FixedChain() {}

    @StateRefinement(from = "a(this)", to = "k == 1 ? b(this) : a(this)")
    public void pick(int k) {}

    @StateRefinement(from = "b(this)", to = "c(this)")
    public void next() {}

    @StateRefinement(from = "c(this)")
    public void done() {}
}
