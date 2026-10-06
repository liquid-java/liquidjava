package testSuite.classes.try_with_resources_correct;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"open", "closed"})
public class Res implements AutoCloseable {
    @StateRefinement(to = "open(this)")
    public Res() {}

    @StateRefinement(from = "open(this)")
    public void read() {}

    @StateRefinement(from = "open(this)", to = "closed(this)")
    public void close() {}
}
