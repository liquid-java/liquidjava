package testSuite.classes.iterator_queue_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.Refinement;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

/**
 * Observer-style iteration over {@link java.util.Scanner} (an iterator-shaped JDK class):
 * {@code hasNext} is an informative check (no state change), and {@code next} consumes one
 * element, resetting state so a fresh check is required before the next consumption.
 *
 * <p>{@code java.util.Iterator} itself is an interface, and external refinements only attach to
 * classes — Scanner gives us the same API in a class form.
 */
@StateSet({"hasMore", "inNext", "notInNext"})
@ExternalRefinementsFor("java.util.Iterator")
public interface IteratorRefinements<N> {

    @StateRefinement(to = "_ --> hasMore()")
    boolean hasNext();

    @StateRefinement(from = "hasMore()", to = "inNext()")
    N next();

    @StateRefinement(from="inNext()", to="notInNext()")
    void remove();
}
