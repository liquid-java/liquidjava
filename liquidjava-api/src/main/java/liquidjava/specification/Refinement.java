package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to add a refinement to variables, class fields, method's parameters and method's
 * return value e.g. @Refinement("x > 0") int x;
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.LOCAL_VARIABLE, ElementType.PARAMETER, ElementType.TYPE})
public @interface Refinement {

    public String value();

    public String msg() default "";
}
