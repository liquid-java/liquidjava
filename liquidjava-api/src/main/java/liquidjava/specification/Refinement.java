package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add a refinement to variables, class fields, method's parameters and method's return values.
 * <p>
 * Refinements are logical predicates that must hold for the annotated element.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @Refinement("x > 0")
 * int x;
 * 
 * @Refinement("_ > 0")
 * int increment(@Refinement("_ >= 0") int n) {
 *     return n + 1;
 * }
 * </pre>
 *
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER, ElementType.TYPE})
public @interface Refinement {

    /**
     * The refinement string that defines a logical predicate that must hold for the annotated element.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @Refinement("x > 0")
     * }
     * </pre>
     */
    String value();

    /**
     * Custom error message to be shown when the refinement is violated (optional).
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @Refinement(value = "x > 0", msg = "x must be positive")
     * }
     * </pre>
     */
    String msg() default "";
}
