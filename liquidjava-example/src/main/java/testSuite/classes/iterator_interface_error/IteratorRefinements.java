package testSuite.classes.iterator_interface_error;

import liquidjava.specification.ExternalRefinementsFor;
import liquidjava.specification.StateRefinement;
import liquidjava.specification.StateSet;

/**
 * External refinements for the JDK interface {@code java.util.Iterator}. Exercises {@code @ExternalRefinementsFor} on an
 * interface target (not a concrete class) and a generic method ({@code N next()} vs the JDK's {@code E next()}).
 */
@StateSet({ "hasMore", "inNext", "notInNext" })
@ExternalRefinementsFor("java.util.Iterator")
public interface IteratorRefinements<N> {

    @StateRefinement(to = "_ --> hasMore()")
    boolean hasNext();

    @StateRefinement(from = "hasMore()", to = "inNext()")
    N next();

    @StateRefinement(from = "inNext()", to = "notInNext()")
    void remove();
}
