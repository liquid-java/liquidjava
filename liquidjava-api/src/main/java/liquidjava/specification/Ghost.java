package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create a ghost variable for a class or interface
 * e.g. `@Ghost("int size")`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(GhostMultiple.class)
public @interface Ghost {

    /**
     * The type and name of the ghost variable
     */
    public String value();
}
