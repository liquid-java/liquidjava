package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to allow the creation of multiple refinement predicates.
 *
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RefinementPredicateMultiple {
    RefinementPredicate[] value();
}
