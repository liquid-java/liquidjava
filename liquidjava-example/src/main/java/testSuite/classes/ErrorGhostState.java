package testSuite.classes;

import liquidjava.specification.Ghost;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "addingItems", "checkout", "closed"})
@Ghost("int totalPrice(int x)") // Error
public class ErrorGhostState {

    @StateRefinement(to = "(totalPrice(this) == 0) && empty(this)")
    public ErrorGhostState() {}
}
