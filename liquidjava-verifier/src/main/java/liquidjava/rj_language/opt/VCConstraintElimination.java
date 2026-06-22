package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;

import liquidjava.processor.VCImplication;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;

/**
 * Removes antecedent constraints that are implied by stronger constraints later in the VC chain
 */
public class VCConstraintElimination implements VCSimplificationPass {

    /**
     * Applies one constraint elimination in a VC chain
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        VCImplication cloned = implication.clone();
        VCImplication simplified = simplify(cloned);
        return simplified == null ? cloned : simplified;
    }

    /**
     * Removes the first antecedent implied by a later antecedent
     */
    private VCImplication simplify(VCImplication implication) {
        if (implication == null || implication.getNext() == null)
            return null;

        VCImplication implying = findImplyingAntecedent(implication);
        if (implying != null)
            return eliminate(implication, implying);

        VCImplication next = simplify(implication.getNext());
        if (next == null)
            return null;

        VCImplication result = copyWithRefinement(implication, implication.getRefinement().clone());
        result.setNext(next);
        return result;
    }

    /**
     * Finds a later antecedent that implies the current constraint. The final node is the conclusion and is not a
     * candidate
     */
    private VCImplication findImplyingAntecedent(VCImplication implication) {
        for (VCImplication candidate = implication.getNext(); candidate != null
                && candidate.getNext() != null; candidate = candidate.getNext()) {
            if (implies(candidate.getRefinement(), implication.getRefinement()))
                return candidate;
        }
        return null;
    }

    /**
     * Eliminates one redundant constraint while preserving any binder attached to it
     */
    private VCImplication eliminate(VCImplication implication, VCImplication implying) {
        if (!implication.hasBinder())
            return implication.getNext().clone();

        VCImplication result = copyWithRefinement(implication, implying.getRefinement().clone());
        result.setNext(remove(implication.getNext(), implying));
        return result;
    }

    /**
     * Removes one node from a suffix
     */
    private VCImplication remove(VCImplication implication, VCImplication target) {
        if (implication == target)
            return implication.getNext() == null ? null : implication.getNext().clone();

        VCImplication result = copyWithRefinement(implication, implication.getRefinement().clone());
        result.setNext(remove(implication.getNext(), target));
        return result;
    }

    /**
     * Checks logical implication using the verifier's existing SMT context
     */
    private boolean implies(Predicate stronger, Predicate weaker) {
        try {
            SMTResult result = new SMTEvaluator().verifySubtype(stronger, weaker, Context.getInstance(), true);
            return result.isOk();
        } catch (Exception e) {
            return false;
        }
    }
}
