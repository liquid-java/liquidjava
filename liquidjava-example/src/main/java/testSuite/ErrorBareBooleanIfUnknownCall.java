package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Regression for issue #371: preserve both outcomes of a boolean condition.
@StateSet({"open", "closed"})
public class ErrorBareBooleanIfUnknownCall {
    @StateRefinement(to = "open(this)")
    public ErrorBareBooleanIfUnknownCall() {}
    @StateRefinement(to = "open(this)")
    public void reopen() {}
    @StateRefinement(to = "closed(this)")
    public void close() {}
    @StateRefinement(from = "open(this)")
    public void use() {}
    public static boolean unknown(boolean value) { return value; }

    public static void check(boolean again) {
        ErrorBareBooleanIfUnknownCall r = new ErrorBareBooleanIfUnknownCall();
        r.close();
        if (unknown(again)) {
            r.reopen();
        }
        r.use(); // Expect: State Refinement Error
    }
}
