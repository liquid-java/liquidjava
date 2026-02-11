package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that allows the creation of ghost variables or refinement aliases within method or constructor scope.
 * <p>
 * This annotation enables the declaration of ghosts and refinement aliases.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @RefinementPredicate("ghost int size")
 * @RefinementPredicate("type Nat(int x) { x > 0 }")
 * public void process() {
 *     // ...
 * }
 * }
 * </pre>
 *
 * @see Ghost
 * @see RefinementAlias
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(RefinementPredicateMultiple.class)
public @interface RefinementPredicate {

    /**
     * The refinement predicate string, which can define a ghost variable or a refinement alias.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @RefinementPredicate("ghost int size")
     * @RefinementPredicate("type Nat(int x) { x > 0 }")
     * }
     * </pre>
     */
    String value();
}
