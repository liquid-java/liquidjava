package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to allow the creation of multiple `@StateSet` annotations
 * e.g. `@StateSets({@StateSet({"open", "reading", "closed"}), @StateSet({"on", "off"})})`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface StateSets {

    /**
     * The array of `@StateSet` annotations to be created
     */
    StateSet[] value();
}
