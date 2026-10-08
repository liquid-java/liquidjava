package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Regression for issue #371: preserve both outcomes of a boolean condition.
@StateSet({"open", "closed"})
public class ErrorBareBooleanIfIndependentConstraint {
    @StateRefinement(to = "open(this)")
    public ErrorBareBooleanIfIndependentConstraint() {}
    @StateRefinement(to = "open(this)")
    public void reopen() {}
    @StateRefinement(to = "closed(this)")
    public void close() {}
    @StateRefinement(from = "open(this)")
    public void use() {}

    @Refinement("n > 0")
    public static boolean query(@Refinement("_ > 0") int n, boolean value) { return value; }

    public static void check(boolean again) {
        ErrorBareBooleanIfIndependentConstraint r = new ErrorBareBooleanIfIndependentConstraint();
        r.close();
        if (query(1, again)) {
            r.reopen();
        }
        r.use(); // Expect: State Refinement Error
    }
}
