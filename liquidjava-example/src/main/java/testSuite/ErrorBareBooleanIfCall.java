package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Regression for issue #371: preserve both outcomes of a boolean condition.
@StateSet({"open", "closed"})
public class ErrorBareBooleanIfCall {
    @StateRefinement(to = "open(this)")
    public ErrorBareBooleanIfCall() {}
    @StateRefinement(to = "open(this)")
    public void reopen() {}
    @StateRefinement(to = "closed(this)")
    public void close() {}
    @StateRefinement(from = "open(this)")
    public void use() {}
    @Refinement("_ == value")
    public static boolean identity(boolean value) { return value; }

    public static void check(boolean again) {
        ErrorBareBooleanIfCall r = new ErrorBareBooleanIfCall();
        r.close();
        if (identity(again)) {
            r.reopen();
        }
        r.use(); // Expect: State Refinement Error
    }
}
