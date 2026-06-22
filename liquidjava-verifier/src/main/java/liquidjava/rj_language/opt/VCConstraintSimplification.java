package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import liquidjava.processor.VCImplication;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.LiteralBoolean;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;

/**
 * Simplifies antecedent constraints that are implied by stronger constraints later in the VC chain
 */
public class VCConstraintSimplification implements VCSimplificationPass {

    /**
     * Applies one constraint simplification in a VC chain
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        VCImplication cloned = implication.clone();
        VCImplication simplified = simplify(cloned);
        return simplified == null ? cloned : simplified;
    }

    /**
     * Simplifies the first antecedent implied by a later antecedent
     */
    private VCImplication simplify(VCImplication implication) {
        if (implication == null || implication.getNext() == null)
            return null;

        if (!isTrue(implication.getRefinement().getExpression())) { // skip trivial constraints
            VCImplication implying = findImplyingAntecedent(implication);
            if (implying != null)
                return simplifyConstraint(implication);
        }

        // continue searching for simplifications in the suffix
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
            // ∀x. x > 0 => x > 1 -> ∀x. true => x > 1
            if (implies(candidate.getRefinement(), implication.getRefinement()))
                return candidate;
        }
        return null;
    }

    /**
     * Simplifies a redundant constraint to true
     */
    private VCImplication simplifyConstraint(VCImplication implication) {
        if (!implication.hasBinder())
            return implication.getNext().clone();

        VCImplication result = copyWithRefinement(implication, new Predicate(new LiteralBoolean(true)));
        result.setNext(implication.getNext().clone());
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
