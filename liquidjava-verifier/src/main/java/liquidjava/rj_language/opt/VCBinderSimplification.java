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
        VCImplication simplified = simplify(cloned, needed.trueBindersToKeep(cloned), needed.unlinkedBinders(cloned));
        return simplified == null ? cloned : simplified;
    }

    /**
     * Simplifies the first applicable binder in a VC chain
     */
    private VCImplication simplify(VCImplication implication, Set<String> keptTrueBinders,
            Set<String> unlinkedBinders) {
        if (implication == null)
            return null;

        if (isFalseBinder(implication))
            return collapseFalseBinder(implication);

        if (isRemovableBinder(implication, keptTrueBinders, unlinkedBinders))
            return removeBinder(implication);

        VCImplication next = simplify(implication.getNext(), keptTrueBinders, unlinkedBinders);
        if (next == null)
            return null;

        return withNext(implication, next);
    }

    /**
     * Copies a VC node followed by its simplified suffix, leaving out a suffix that became just true
     */
    private VCImplication withNext(VCImplication implication, VCImplication next) {
        VCImplication result = copyWithRefinement(implication, implication.getRefinement().clone());
        // Found reads its rows as a conjunction, so P && true -> P: a trailing true row only reads as the value above
        if (!isPlainTrue(next))
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
     * unless the error needs it. A generated path condition that expected does not depend on is removed too
     */
    private boolean isRemovableBinder(VCImplication implication, Set<String> keptTrueBinders,
            Set<String> unlinkedBinders) {
        if (!implication.hasBinder())
            return false;

        if (isTrueBinder(implication))
            return !keptTrueBinders.contains(implication.getName());
        if (isGeneratedBooleanBinder(implication) && unlinkedBinders.contains(implication.getName()))
            return true;
        return !containsVar(implication.getNext(), implication.getName()) && isUnusedFreshPathBinder(implication);
    }

    /**
     * Checks for a generated boolean path binder refined exactly by itself
     */
    private boolean isUnusedFreshPathBinder(VCImplication implication) {
        if (!implication.hasNext() || !isGeneratedBooleanBinder(implication))
            return false;

        return implication.getRefinement().getExpression()instanceof Var var
                && implication.getName().equals(var.getName());
    }

    /**
     * Checks for a binder of a generated boolean, such as a path condition
     */
    private boolean isGeneratedBooleanBinder(VCImplication implication) {
        return GeneratedNames.isGenerated(implication.getName())
                && "boolean".equals(implication.getType().getQualifiedName());
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
     * Checks whether a VC node is a last implication without binder refined with true
     */
    private boolean isPlainTrue(VCImplication implication) {
        return !implication.hasBinder() && !implication.hasNext()
                && isTrue(implication.getRefinement().getExpression());
    }

    /**
     * Checks whether a VC node is a binder refined with false
     */
    private boolean isFalseBinder(VCImplication implication) {
        return implication.hasBinder() && isFalse(implication.getRefinement().getExpression());
    }
}
