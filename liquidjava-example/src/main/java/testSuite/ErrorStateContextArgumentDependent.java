package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
public class ErrorStateContextArgumentDependent {

    @StateRefinement(to = "open(this)")
    public ErrorStateContextArgumentDependent() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    // the states that allow it depend on its argument
    @StateRefinement(from = "closed(this) && attempts > 0", to = "open(this)")
    public void reopen(int attempts) {}

    public static void main(String[] args) {
        ErrorStateContextArgumentDependent f = new ErrorStateContextArgumentDependent();
        f.reopen(1); // Expect: State Refinement Error
    }
}
