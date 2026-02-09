package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that allows the creation of ghosts or refinement aliases
 * e.g. `@RefinementPredicate("ghost int size")`, `@RefinementPredicate("type Nat(int x) { x > 0 }")`
 * 
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(RefinementPredicateMultiple.class)
public @interface RefinementPredicate {

    /**
     * The refinement predicate string, which can define a ghost variable or a refinement alias
     */
    public String value();
}
