package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// the call needs either entry state: the error declares both, each where its annotation writes it
@StateSet(name = "door", value = {"shut", "ajar", "wide"})
public class ErrorStateRefinementEveryFrom {

    @StateRefinement(to = "wide(this)")
    public ErrorStateRefinementEveryFrom() {}

    @StateRefinement(from = "shut(this)", to = "ajar(this)")
    @StateRefinement(from = "ajar(this)", to = "ajar(this)")
    public void push() {}

    public static void main(String[] args) {
        new ErrorStateRefinementEveryFrom().push(); // Expect: State Refinement Error
    }
}
