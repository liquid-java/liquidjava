package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create a set of disjoint states in which class objects can be.
 * <p>
 * An object will always be in exactly one state from each state set at any given time,
 * and cannot be in more than one state from the same state set (e.g., {@code open} and {@code closed} simultaneously).
 * To allow an object to be in multiple states at once, they must be from different state sets.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @StateSet({"open", "reading", "closed"})
 * @StateSet({"locked", "unlocked"})
 * public class File {
 *    // ...
 * }
 * </pre>
 *
 * @see StateRefinement
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(StateSet.Multiple.class)
public @interface StateSet {

    /**
     * The array of states to be created.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @StateSet({"open", "reading", "closed"})
     * }
     * </pre>
     */
    String[] value();

    /**
     * Container annotation used by {@link Repeatable} to support multiple state sets.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface Multiple {
        StateSet[] value();
    }
}
