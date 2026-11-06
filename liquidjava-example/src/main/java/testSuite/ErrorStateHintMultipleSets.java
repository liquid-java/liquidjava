package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet(name = "doorState", value = {"closed", "opened"})
@StateSet(name = "lockState", value = {"unlocked", "locked"})
public class ErrorStateHintMultipleSets {

    @StateRefinement(to = "closed(this) && unlocked(this)")
    public ErrorStateHintMultipleSets() {}

    @StateRefinement(from = "closed(this) && unlocked(this)", to = "opened(this)")
    public void open() {}

    @StateRefinement(from = "opened(this)", to = "closed(this)")
    public void close() {}

    @StateRefinement(from = "closed(this) && unlocked(this)", to = "locked(this)")
    public void lock() {}

    @StateRefinement(from = "locked(this)", to = "unlocked(this)")
    public void unlock() {}

    public static void main(String[] args) {
        ErrorStateHintMultipleSets d = new ErrorStateHintMultipleSets();
        d.lock();
        d.open(); // Expect: State Refinement Error
    }
}
