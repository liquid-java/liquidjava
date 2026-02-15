package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to refine a class or interface of an external library.
 * <p>
 * This annotation allows you to specify refinements and state transitions for classes from external libraries
 * that you cannot directly annotate. The refinements apply to all instances of the specified class.
 * <p>
 * <strong>Example:</strong>
 * <pre>
 * {@code
 * @ExternalRefinementsFor("java.lang.Math")
 * public interface MathRefinements {
 *     @Refinement("_ >= 0")
 *     public double sqrt(double x);
 * }
 * }
 * </pre>
 *
 * @author Catarina Gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ExternalRefinementsFor {
    
    /**
     * The fully qualified name of the class or interface for which the refinements are being defined.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * @ExternalRefinementsFor("java.lang.Math")
     * }
     * </pre>
     */
    String value();
}
