package testSuite.classes.inherited_overload_error;

import liquidjava.specification.*;

@ExternalRefinementsFor("java.util.Stack")
@StateSet({"ready", "changed"})
public interface StackRefinements<E> {
    @StateRefinement(to = "ready(this)") void Stack();
    @StateRefinement(to = "changed(this)") E remove(int index);
    @StateRefinement(from = "ready(this)") E peek();
}
