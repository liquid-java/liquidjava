package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create a refinement alias to be reused in refinements.
 * <p>
 * Refinement aliases can be used to define reusable refinement predicates with parameters.
 * They help reduce duplication and improve readability of complex refinement specifications.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @RefinementAlias("Nat(int x) { x > 0 }")
 * public class MyClass {
 *     @Refinement("Nat(_)")
 *     int value;
 * }
 * }
 * </pre>
 *
 * @see Refinement
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(RefinementAlias.Multiple.class)
public @interface RefinementAlias {

    /**
     * The refinement alias string, which includes the name of the alias, its parameters and the refinement itself.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @RefinementAlias("Nat(int x) { x > 0 }")
     * }
     * </pre>
     */
    String value();

    /**
     * Container annotation used by {@link Repeatable} to support multiple refinement aliases.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @interface Multiple {
        RefinementAlias[] value();
    }
}
