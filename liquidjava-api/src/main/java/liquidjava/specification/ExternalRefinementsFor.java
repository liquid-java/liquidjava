package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to refine a class or interface of an external library
 * e.g. `@ExternalRefinementsFor("java.lang.Math")`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ExternalRefinementsFor {
    
    /**
     * The fully qualified name of the class or interface for which the refinements are being defined
     */
    public String value();
}
