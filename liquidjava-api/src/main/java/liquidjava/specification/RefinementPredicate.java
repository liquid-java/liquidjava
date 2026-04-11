package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that allows the creation of custom ghost functions within classes or interfaces.
 * <p>
 * This annotation enables the declaration of custom ghost functions.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @RefinementPredicate("int totalPrice(Order o)")
 * public class Order {
 *     @StateRefinement(to = "totalPrice(this) == 0")
 *     public Order() {
 *     }
 * }
 * }
 * </pre>
 *
 * @see Ghost
 * @see RefinementAlias
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(RefinementPredicate.Multiple.class)
public @interface RefinementPredicate {

    /**
     * The refinement predicate string, which defines a ghost function declaration.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @RefinementPredicate("int totalPrice(Order o)")
     * }
     * </pre>
     */
    String value();

    /**
     * Container annotation used by {@link Repeatable} to support multiple refinement predicates.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface Multiple {
        RefinementPredicate[] value();
    }
}
