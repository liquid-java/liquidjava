package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.containsVar;
import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isFalse;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import java.util.Set;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.Var;
import liquidjava.utils.GeneratedNames;

/**
 * Simplifies VCImplication chains by removing vacuous binder implications
 */
public class VCBinderSimplification implements VCSimplificationPass {

    private final NeededVariables needed;

    public VCBinderSimplification() {
        this(NeededVariables.NONE);
    }

    public VCBinderSimplification(NeededVariables needed) {
        this.needed = needed;
    }

    /**
     * Applies one binder simplification in a VC chain
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        VCImplication cloned = implication.clone();
        VCImplication simplified = simplify(cloned, needed.trueBindersToKeep(cloned));
        return simplified == null ? cloned : simplified;
    }

    /**
     * Simplifies the first applicable binder in a VC chain
     */
    private VCImplication simplify(VCImplication implication, Set<String> keptTrueBinders) {
        if (implication == null)
            return null;

        if (isFalseBinder(implication))
            return collapseFalseBinder(implication);

        if (isRemovableBinder(implication, keptTrueBinders))
            return removeBinder(implication);

        VCImplication next = simplify(implication.getNext(), keptTrueBinders);
        if (next == null)
            return null;

        VCImplication result = copyWithRefinement(implication, implication.getRefinement().clone());
        result.setNext(next);
        return result;
    }

    /**
     * Removes a binder that can be omitted from the suffix
     */
    private VCImplication removeBinder(VCImplication implication) {
        VCImplication next = implication.getNext();

        // ∀x. true => P -> P, and unneeded generated path conditions can be omitted from diagnostics
        if (next != null)
            return next.clone();

        // ∀x. true -> true
        Predicate truePredicate = new Predicate(new LiteralBoolean(true));
        return new VCImplication(truePredicate);
    }

    /**
     * Checks whether a binder can be removed without changing the VC conclusion or hiding what explains the error. A
     * binder refined only by true states no fact, so it is removed even when a later implication uses its variable,
     * unless the error needs it
     */
    private boolean isRemovableBinder(VCImplication implication, Set<String> keptTrueBinders) {
        if (!implication.hasBinder())
            return false;

        if (isTrueBinder(implication))
            return !keptTrueBinders.contains(implication.getName());
        return !containsVar(implication.getNext(), implication.getName()) && isUnusedFreshPathBinder(implication);
    }

    /**
     * Checks for a generated boolean path binder refined exactly by itself
     */
    private boolean isUnusedFreshPathBinder(VCImplication implication) {
        if (!implication.hasNext() || !GeneratedNames.isGenerated(implication.getName())
                || !"boolean".equals(implication.getType().getQualifiedName()))
            return false;

        return implication.getRefinement().getExpression()instanceof Var var
                && implication.getName().equals(var.getName());
    }

    /**
     * Replaces a false binder implication with true
     */
    private VCImplication collapseFalseBinder(VCImplication implication) {
        // ∀x. false => P -> true
        Predicate truePredicate = new Predicate(new LiteralBoolean(true));
        return new VCImplication(truePredicate);
    }

    /**
     * Checks whether a VC node is a binder refined with true
     */
    private boolean isTrueBinder(VCImplication implication) {
        return implication.hasBinder() && isTrue(implication.getRefinement().getExpression());
    }

    /**
     * Checks whether a VC node is a binder refined with false
     */
    private boolean isFalseBinder(VCImplication implication) {
        return implication.hasBinder() && isFalse(implication.getRefinement().getExpression());
    }
}
