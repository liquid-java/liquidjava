package testSuite;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"a", "b", "c"})
public class ErrorStateHintSubclass {

    @StateRefinement(to = "a(this)")
    public ErrorStateHintSubclass() {}

    @StateRefinement(from = "a(this)", to = "b(this)")
    public void ab() {}

    @StateRefinement(from = "c(this)")
    public void useC() {}

    public static void use() {
        ErrorStateHintSubclass s = new ErrorStateHintSubclassChild();
        s.ab();
        s.useC(); // Expect: State Refinement Error
    }
}

// adds a transition the superclass does not know about, so the hints cannot claim that c is unreachable
class ErrorStateHintSubclassChild extends ErrorStateHintSubclass {

    @StateRefinement(from = "b(this)", to = "c(this)")
    public void bc() {}
}
