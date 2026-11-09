package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet(name = "door", value = {"closed", "opened"})
@StateSet(name = "lock", value = {"unlocked", "locked"})
@RefinementAlias("Shut(CorrectStateValues d) { door(d) == closed }")
public class CorrectStateValues {

    @StateRefinement(to = "door(this) == closed && lock(this) == unlocked")
    public CorrectStateValues() {}

    @StateRefinement(from = "door(this) == closed && lock(this) != locked", to = "door(this) == opened")
    public void open() {}

    @StateRefinement(from = "door(this) == opened", to = "door(this) == closed")
    public void close() {}

    // when it fails, the lock set keeps its value
    @StateRefinement(from = "door(this) == closed", to = "return ? lock(this) == locked : lock(this) == lock(old(this))")
    public boolean tryLock() {
        return true;
    }

    @StateRefinement(from = "lock(this) == locked", to = "lock(this) == unlocked")
    public void unlock() {}

    // the parameter closed keeps its meaning: the door takes its value
    @StateRefinement(to = "door(this) == closed")
    public void choose(int closed) {}

    static void needsShut(@Refinement("Shut(_)") CorrectStateValues d) {}

    public static void main(String[] args) {
        CorrectStateValues d = new CorrectStateValues();
        int closed = 1; // the alias body still means the state closed
        needsShut(d);
        d.open();
        d.close();
        if (d.tryLock())
            d.unlock();
        d.open();
        d.choose(1);
        d.close();
    }
}

// states named like the variables _ and return do not take their place
@StateSet(name = "mode", value = {"_", "return", "on"})
class CorrectStateValueVariableNames {

    @StateRefinement(to = "mode(this) == on")
    public CorrectStateValueVariableNames() {}

    public void check(@Refinement("mode(this) == _") int value) {}

    @Refinement("_ == 2")
    @StateRefinement(to = "mode(this) == return")
    public int pick() {
        return 2;
    }

    @StateRefinement(from = "mode(this) == on")
    public void needsOn() {}

    static void test() {
        CorrectStateValueVariableNames v = new CorrectStateValueVariableNames();
        v.check(2);
        v.pick();
        v.needsOn();
    }
}
