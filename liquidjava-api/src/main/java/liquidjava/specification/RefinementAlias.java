package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create a refinement alias
 * e.g. `@RefinementAlias("Nat(int x) { x > 0 }")`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Repeatable(RefinementAliasMultiple.class)
public @interface RefinementAlias {

    /**
     * The refinement alias string, which includes the name of the alias, its parameters and the refinement itself
     */
    public String value();
}
