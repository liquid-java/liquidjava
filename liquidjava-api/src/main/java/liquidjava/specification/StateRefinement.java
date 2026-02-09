package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create state transitions in a method
 * e.g. `@StateRefinement(from="open(this)", to="closed(this)", msg="The object needs to be open before closing")`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(StateRefinementMultiple.class)
public @interface StateRefinement {

    /**
     * The state in which the object needs to be before calling the method
     */
    public String from() default "";

    /**
     * The state in which the object will be after calling the method
     */
    public String to() default "";

    /**
     * Optional custom error message to be included in the error message when the `from` is violated
     */
    public String msg() default "";
}
