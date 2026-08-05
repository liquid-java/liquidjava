package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.containsVar;
import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isFalse;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.rj_language.ast.Var;

/**
 * Simplifies VCImplication chains by removing vacuous binder implications
 */
public class VCBinderSimplification implements VCSimplificationPass {

    private static final String FRESH_PREFIX = "#fresh_";

    /**
     * Applies one binder simplification in a VC chain
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        VCImplication cloned = implication.clone();
        VCImplication simplified = simplify(cloned);
        return simplified == null ? cloned : simplified;
    }

    /**
     * Simplifies the first applicable binder in a VC chain
     */
    private VCImplication simplify(VCImplication implication) {
        if (implication == null)
            return null;

        if (isFalseBinder(implication))
            return collapseFalseBinder(implication);

        if (isRemovableUnusedBinder(implication))
            return removeBinder(implication);

        VCImplication next = simplify(implication.getNext());
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

        // ∀x. true => P -> P, and unused generated path conditions can be omitted from diagnostics
        if (next != null)
            return next.clone();

        // ∀x. true -> true
        Predicate truePredicate = new Predicate(new LiteralBoolean(true));
        return new VCImplication(truePredicate);
    }

    /**
     * Checks whether a binder is unused and can be removed without changing the VC conclusion
     */
    private boolean isRemovableUnusedBinder(VCImplication implication) {
        if (!implication.hasBinder() || containsVar(implication.getNext(), implication.getName()))
            return false;

        return isTrueBinder(implication) || isUnusedFreshPathBinder(implication);
    }

    /**
     * Checks for a generated boolean path binder refined exactly by itself
     */
    private boolean isUnusedFreshPathBinder(VCImplication implication) {
        if (!implication.hasNext() || !implication.getName().startsWith(FRESH_PREFIX)
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
