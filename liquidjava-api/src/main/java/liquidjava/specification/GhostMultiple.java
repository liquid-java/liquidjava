package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to allow the creation of multiple `@Ghost` annotations in a class or interface
 * e.g. `@GhostMultiple({@Ghost("int size"), @Ghost("boolean isEmpty")})`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface GhostMultiple {

    /**
     * The array of `@Ghost` annotations to be created
     */
    Ghost[] value();
}
