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
            if (isTrue(redundant.getRefinement().getExpression()))
                continue;

            for (VCImplication stronger = implication; stronger.getNext() != null; stronger = stronger.getNext()) {
                if (stronger != redundant && canEliminate(implication, stronger, redundant)
                        && implies(stronger.getRefinement(), redundant.getRefinement()))
                    return eliminate(implication, stronger, redundant);
            }
        }
        return null;
    }

    /**
     * Checks whether either node can be removed while preserving required binders
     */
    private boolean canEliminate(VCImplication implication, VCImplication stronger, VCImplication redundant) {
        return isRemovable(implication, redundant) || isRelocatable(implication, stronger);
    }

    /**
     * Removes the redundant node, or moves the stronger refinement onto its required binder and removes the stronger
     * node
     */
    private VCImplication eliminate(VCImplication implication, VCImplication stronger, VCImplication redundant) {
        if (isRemovable(implication, redundant))
            return rewrite(implication, redundant, null, null);
        return rewrite(implication, stronger, redundant, stronger.getRefinement());
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
     * Checks whether a node can be removed while retaining its refinement elsewhere
     */
    private boolean isRelocatable(VCImplication implication, VCImplication node) {
        return isRemovable(implication, node)
                && (!node.hasBinder() || !containsVar(node.getRefinement().getExpression(), node.getName()));
    }

    /**
     * Clones a chain while removing one node and optionally replacing another node's refinement
     */
    private VCImplication rewrite(VCImplication implication, VCImplication removed, VCImplication replaced,
            Predicate replacement) {
        if (implication == null)
            return null;
        if (implication == removed)
            return rewrite(implication.getNext(), removed, replaced, replacement);

        Predicate refinement = implication == replaced ? replacement.clone() : implication.getRefinement().clone();
        VCImplication result = copyWithRefinement(implication, refinement);
        result.setNext(rewrite(implication.getNext(), removed, replaced, replacement));
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
