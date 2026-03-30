package testSuite.classes.state_refinement_no_to_typestate_correct;

import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

@StateSet({"empty", "one", "many"})
public class Counter {

    @StateRefinement(to="empty(this)")
    public Counter() {}

    @StateRefinement(from="empty(this)", to="one(this)")
    public void addFirst() {}

    @StateRefinement(from="one(this)", to="many(this)")
    public void addSecond() {}

    @StateRefinement(from="one(this) || many(this)")
    public void inspect() {}

    @StateRefinement(from="many(this)")
    public void consumeOne() {}
}
