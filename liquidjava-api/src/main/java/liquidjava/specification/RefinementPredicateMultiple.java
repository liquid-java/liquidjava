package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to allow the creation of multiple `@RefinementPredicate` annotations
 * e.g. `@RefinementPredicateMultiple({`@RefinementPredicate("ghost int size")`, `@RefinementPredicate("type Nat(int x) { x > 0 }")`})`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface RefinementPredicateMultiple {

    /**
     * The array of `@RefinementPredicate` annotations to be created
     */
    RefinementPredicate[] value();
}
