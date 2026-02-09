package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to allow the creation of multiple `@StateRefinement` annotations
 * e.g. `@StateRefinementMultiple({@StateRefinement(from="open(this)", to="closed(this)"), @StateRefinement(from="closed(this)", to="open(this)")})`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface StateRefinementMultiple {

    /**
     * The array of `@StateRefinement` annotations to be created
     */
    StateRefinement[] value();
}
