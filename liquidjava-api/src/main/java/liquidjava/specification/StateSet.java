package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create a set of disjoint states in which class objects can be.
 * <p>
 * An object cannot be in more than one state from the same state set at a time (e.g., {@code open} and
 * {@code closed} simultaneously). To allow an object to be in multiple states at once, they must be from different
 * state sets. An object may also be in none of the states of a set, e.g. after
 * {@code @StateRefinement(to = "!open(this) && !closed(this)")}.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @StateSet({"open", "reading", "closed"})
 * @StateSet({"locked", "unlocked"})
 * public class File {
 *    // ...
 * }
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
     * Optional name of the state set, used in messages and usable in predicates as a function that gives the current
     * state of the set (e.g. {@code redo(this) == old(redo(this))}). It must be a lowercase identifier that is not
     * already the name of a state, ghost or other state set of the class. Unnamed sets get a generated name.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @StateSet(name = "redo", value = {"redoable", "notRedoable"})
     * }
     * </pre>
     */
    String name() default "";

    /**
     * Container annotation used by {@link Repeatable} to support multiple state sets.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface Multiple {
        StateSet[] value();
    }
}
