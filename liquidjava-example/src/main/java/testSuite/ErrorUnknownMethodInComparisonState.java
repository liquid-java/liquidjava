package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Calling a method without refinements in a condition keeps the object's state (issue #300)
@StateSet({"open", "closed"})
public class ErrorUnknownMethodInComparisonState {
    @StateRefinement(to = "open(this)")
    public ErrorUnknownMethodInComparisonState() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    public int count() {
        return 0;
    }

    public static void main(String[] args) {
        ErrorUnknownMethodInComparisonState r = new ErrorUnknownMethodInComparisonState();
        r.close();
        if (r.count() > 0) {
            System.out.println("non-empty");
        }
        r.close(); // Expect: State Refinement Error
    }
}
