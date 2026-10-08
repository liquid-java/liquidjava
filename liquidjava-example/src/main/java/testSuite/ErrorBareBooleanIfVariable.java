package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Regression for issue #371: preserve both outcomes of a boolean condition.
@StateSet({"open", "closed"})
public class ErrorBareBooleanIfVariable {
    @StateRefinement(to = "open(this)")
    public ErrorBareBooleanIfVariable() {}
    @StateRefinement(to = "open(this)")
    public void reopen() {}
    @StateRefinement(to = "closed(this)")
    public void close() {}
    @StateRefinement(from = "open(this)")
    public void use() {}

    public static void check(boolean again) {
        ErrorBareBooleanIfVariable r = new ErrorBareBooleanIfVariable();
        r.close();
        if (again) {
            r.reopen();
        }
        r.use(); // Expect: State Refinement Error
    }
}
