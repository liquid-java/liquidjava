package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"closed", "opened"})
@StateSet({"unlocked", "locked"})
public class ErrorPathDependentOtherStateSet {

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public ErrorPathDependentOtherStateSet() {}

    @StateRefinement(from = "closed(this)", to = "opened(this)")
    public void open() {}

    @StateRefinement(from = "unlocked(this)", to = "locked(this)")
    public void lock() {}

    @StateRefinement(from = "opened(this)")
    public void pass() {}

    // the states differ only in the lock, which pass() does not check, so the path is not what is wrong
    public static void use(boolean secure) {
        ErrorPathDependentOtherStateSet d = new ErrorPathDependentOtherStateSet();
        if (secure)
            d.lock();
        d.pass(); // Expect: State Refinement Error
    }
}
