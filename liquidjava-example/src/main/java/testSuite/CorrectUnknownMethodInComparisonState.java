package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// Calling a method without refinements in a condition keeps the object's state (issue #300)
@StateSet({"open", "closed"})
public class CorrectUnknownMethodInComparisonState {
    @StateRefinement(to = "open(this)")
    public CorrectUnknownMethodInComparisonState() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}

    public int count() {
        return 0;
    }

    public static void main(String[] args) {
        CorrectUnknownMethodInComparisonState r = new CorrectUnknownMethodInComparisonState();
        if (r.count() > 0) {
            System.out.println("non-empty");
        }
        r.close();
    }
}
