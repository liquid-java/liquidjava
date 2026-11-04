package testSuite.classes.generated_names_error;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"opened", "closed"})
public class Door {

    @StateRefinement(to = "closed(this)")
    public Door() {}

    @StateRefinement(from = "opened(this)", to = "closed(this)")
    public void close() {}
}
