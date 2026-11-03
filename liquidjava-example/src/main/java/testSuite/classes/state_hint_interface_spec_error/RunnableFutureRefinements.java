package testSuite.classes.state_hint_interface_spec_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

// A spec for an interface: it has no constructor, so no new object is suggested
@ExternalRefinementsFor("java.util.concurrent.RunnableFuture")
@StateSet({"pending", "done"})
public interface RunnableFutureRefinements {

    @StateRefinement(from = "pending(this)", to = "done(this)")
    void run();

    @StateRefinement(from = "done(this)")
    boolean isDone();
}
