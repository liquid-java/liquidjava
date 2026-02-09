package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create the disjoint states in which class objects can be
 * e.g. @StateSet({"open", "reading", "closed"})
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(StateSets.class)
public @interface StateSet {

    /**
     * The array of states to be created
     */
    public String[] value();
}
