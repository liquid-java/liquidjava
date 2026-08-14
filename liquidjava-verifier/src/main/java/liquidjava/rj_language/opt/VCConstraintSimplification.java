package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.containsVar;
import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;

import liquidjava.processor.VCImplication;
import liquidjava.processor.context.Context;
import liquidjava.rj_language.Predicate;
import liquidjava.smt.SMTEvaluator;
import liquidjava.smt.SMTResult;

/**
 * Removes antecedent constraints that are implied by another antecedent
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
     * Removes the first antecedent implied by another antecedent
     */
    private VCImplication simplify(VCImplication implication) {
        for (VCImplication redundant = implication; redundant.getNext() != null; redundant = redundant.getNext()) {
            if (isTrue(redundant.getRefinement().getExpression()) || !isRemovable(implication, redundant))
                continue;

            for (VCImplication stronger = implication; stronger.getNext() != null; stronger = stronger.getNext()) {
                if (stronger != redundant && implies(stronger.getRefinement(), redundant.getRefinement()))
                    return remove(implication, redundant);
            }
        }
        return null;
    }

    /**
     * Checks whether removing a node also removes every use of its binder
     */
    private boolean isRemovable(VCImplication implication, VCImplication node) {
        if (!node.hasBinder())
            return true;

        for (VCImplication current = implication; current != null; current = current.getNext())
            if (current != node && containsVar(current.getRefinement().getExpression(), node.getName()))
                return false;
        return true;
    }

    /**
     * Clones a chain while removing one node
     */
    private VCImplication remove(VCImplication implication, VCImplication removed) {
        if (implication == removed)
            return implication.getNext().clone();

        VCImplication result = copyWithRefinement(implication, implication.getRefinement().clone());
        result.setNext(remove(implication.getNext(), removed));
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
