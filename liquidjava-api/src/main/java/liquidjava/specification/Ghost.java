package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create a ghost variable for a class or interface.
 * <p>
 * Ghost variables that only exist during the verification and can be used in refinements and state refinements.
 * They are not part of the actual implementation but help specify behavior and invariants.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @Ghost("int size")
 * public class MyStack {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(GhostMultiple.class)
public @interface Ghost {

    /**
     * The type and name of the ghost variable.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @Ghost("int size")
     * }
     * </pre>
     */
    String value();
}
