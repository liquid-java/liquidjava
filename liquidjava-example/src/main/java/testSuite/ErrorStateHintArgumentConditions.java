package testSuite;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A call whose state after depends on its arguments is suggested with the condition its refinement tests, only when
// every argument that meets the condition leads to a state that allows the failing call
public class ErrorStateHintArgumentConditions {

    // a condition on two parameters
    static void twoParameters() {
        ConditionRange r = new ConditionRange();
        r.scan(); // Expect: State Refinement Error
    }

    // a condition, then a call that needs no condition
    static void thenAnotherCall() {
        ConditionChain chain = new ConditionChain();
        chain.done(); // Expect: State Refinement Error
    }

    // the negation of the condition the refinement tests
    static void negatedCondition() {
        ConditionLimit l = new ConditionLimit();
        l.stream(); // Expect: State Refinement Error
    }

    // the condition does not decide the state
    static void undecided() {
        ConditionLoose l = new ConditionLoose();
        l.go(); // Expect: State Refinement Error
    }

    // nor does an argument value that meets it: no value is suggested for the earlier call
    static void undecidedValue() {
        ConditionLoose l = new ConditionLoose();
        l.set(1);
        l.go(); // Expect: State Refinement Error
    }

    // the condition mentions a ghost of this, and then of another parameter: not a condition on the arguments alone
    static void ghostOfThis() {
        ConditionSized s = new ConditionSized();
        s.shrink(); // Expect: State Refinement Error
    }

    static void ghostOfParameter() {
        ConditionBin b = new ConditionBin();
        b.dump(); // Expect: State Refinement Error
    }
}

@StateSet({"low", "high"})
class ConditionRange {
    @StateRefinement(to = "low(this)")
    public ConditionRange() {}

    @StateRefinement(to = "lo < hi ? high(this) : low(this)")
    public void set(int lo, int hi) {}

    @StateRefinement(from = "high(this)")
    public void scan() {}
}

@StateSet({"a", "b", "c"})
class ConditionChain {
    @StateRefinement(to = "a(this)")
    public ConditionChain() {}

    @StateRefinement(from = "a(this)", to = "k == 1 ? b(this) : a(this)")
    public void pick(int k) {}

    @StateRefinement(from = "b(this)", to = "c(this)")
    public void next() {}

    @StateRefinement(from = "c(this)")
    public void done() {}
}

@StateSet({"bounded", "unbounded"})
class ConditionLimit {
    @StateRefinement(to = "bounded(this)")
    public ConditionLimit() {}

    @StateRefinement(to = "limit > 0 ? bounded(this) : unbounded(this)")
    public void setLimit(int limit) {}

    @StateRefinement(from = "unbounded(this)")
    public void stream() {}
}

@StateSet({"a", "b"})
class ConditionLoose {
    @StateRefinement(to = "b(this)")
    public ConditionLoose() {}

    @StateRefinement(to = "mode == 2 ? (a(this) || b(this)) : b(this)")
    public void set(int mode) {}

    @StateRefinement(from = "a(this)")
    public void go() {}
}

@Ghost("int size")
@StateSet({"small", "big"})
class ConditionSized {
    @StateRefinement(to = "small(this) && size(this) == 0")
    public ConditionSized() {}

    @StateRefinement(to = "n > size(old(this)) ? big(this) : small(this)")
    public void grow(int n) {}

    @StateRefinement(from = "big(this)")
    public void shrink() {}
}

@Ghost("int capacity")
@StateSet({"empty", "full"})
class ConditionBin {
    @StateRefinement(to = "empty(this)")
    public ConditionBin() {}

    @StateRefinement(from = "empty(this)", to = "capacity(other) > 3 ? full(this) : empty(this)")
    public void fill(ConditionBin other) {}

    @StateRefinement(from = "full(this)")
    public void dump() {}
}
