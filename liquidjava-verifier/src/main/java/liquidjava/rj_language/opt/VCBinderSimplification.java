package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.LiteralBoolean;

/**
 * Simplifies VCImplication chains by removing vacuous binder implications
 */
public class VCBinderSimplification implements VCSimplificationPass {

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

        if (isTrueBinder(implication) && !containsVar(implication.getNext(), implication.getName()))
            return removeTrueBinder(implication);

        VCImplication next = simplify(implication.getNext());
        if (next == null)
            return null;

        VCImplication result = implication.copyWithRefinement(implication.getRefinement().clone());
        result.setNext(next);
        return result;
    }

    /**
     * Removes a true binder whose name is not used in the suffix
     */
    private VCImplication removeTrueBinder(VCImplication implication) {
        VCImplication next = implication.getNext();

        // ∀x. true => P -> P
        if (next != null) {
            VCImplication origin = new VCImplication(implication.getName(), implication.getType(),
                    next.getOriginRefinement());
            VCImplication result = new SimplifiedVCImplication(next, next.getRefinement().clone(), origin);
            result.setNext(next.getNext() == null ? null : next.getNext().clone());
            return result;
        }

        // ∀x. true -> true
        Predicate truePredicate = new Predicate(new LiteralBoolean(true));
        return new SimplifiedVCImplication(new VCImplication(truePredicate), truePredicate, implication);
    }

    /**
     * Replaces a false binder implication with true
     */
    private VCImplication collapseFalseBinder(VCImplication implication) {
        // ∀x. false => P -> true
        Predicate truePredicate = new Predicate(new LiteralBoolean(true));
        return new SimplifiedVCImplication(new VCImplication(truePredicate), truePredicate, implication);
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

    /**
     * Checks whether an expression is true
     */
    private boolean isTrue(Expression expression) {
        return expression instanceof LiteralBoolean literal && literal.isBooleanTrue();
    }

    /**
     * Checks whether an expression is false
     */
    private boolean isFalse(Expression expression) {
        return expression instanceof LiteralBoolean literal && !literal.isBooleanTrue();
    }

    /**
     * Checks whether a VC suffix contains a variable name
     */
    private boolean containsVar(VCImplication implication, String name) {
        for (VCImplication current = implication; current != null; current = current.getNext()) {
            List<String> names = new ArrayList<>();
            current.getRefinement().getExpression().getVariableNames(names);
            if (names.contains(name))
                return true;
        }
        return false;
    }
}
