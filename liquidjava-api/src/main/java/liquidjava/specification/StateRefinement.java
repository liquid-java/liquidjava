package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create state transitions in a method using states defined in state sets.
 * <p>
 * This annotation specifies the required precondition state and the resulting
 * postcondition state when a method or constructor is invoked.
 * Constructors can only specify the postcondition state since they create a new object.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @StateRefinement(from="open(this)", to="closed(this)", msg="The object needs to be open before closing")
 * public void close() {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @see StateSet
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(StateRefinementMultiple.class)
public @interface StateRefinement {

    /**
     * The logical pre-condition that defines the state in which the object needs to be before calling the method (optional)
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @StateRefinement(from="open(this)")
     * }
     * </pre>
     */
    String from() default "";

    /**
     * The logical post-condition that defines the state in which the object will be after calling the method (optional)
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @StateRefinement(from="open(this)", to="closed(this)")
     * }
     * </pre>
     */
    String to() default "";

    /**
     * Custom error message to be shown when the {@code from} pre-condition is violated (optional)
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @StateRefinement(from="open(this)", to="closed(this)", msg="The object needs to be open before closing")
     * }
     * </pre>
     */
    String msg() default "";
}
