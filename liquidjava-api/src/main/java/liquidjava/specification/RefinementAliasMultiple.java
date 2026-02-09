package liquidjava.specification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to create multiple refinement aliases
 * e.g. `@RefinementAliasMultiple({@RefinementAlias("Nat(int x) { x > 0 }"), @RefinementAlias("Pos(int x) { x >= 0 }")})`
 *
 * @author catarina gamboa
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface RefinementAliasMultiple {

    /**
     * The array of `@RefinementAlias` annotations to be created
     */
    RefinementAlias[] value();
}
