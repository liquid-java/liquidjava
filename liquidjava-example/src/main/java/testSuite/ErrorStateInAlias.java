package testSuite;

import liquidjava.specification.Refinement;
import liquidjava.specification.RefinementAlias;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"closed", "opened"})
@RefinementAlias("Closed(ErrorStateInAlias x) { closed(x) }")
public class ErrorStateInAlias {

    @StateRefinement(to = "closed(this)")
    public ErrorStateInAlias() {}

    @StateRefinement(from = "closed(this)", to = "opened(this)")
    public void open() {}

    static void needsClosed(@Refinement("Closed(_)") ErrorStateInAlias door) {}

    public static void main(String[] args) {
        ErrorStateInAlias door = new ErrorStateInAlias();
        needsClosed(door);
        door.open();
        needsClosed(door); // Expect: Refinement Error
    }
}
