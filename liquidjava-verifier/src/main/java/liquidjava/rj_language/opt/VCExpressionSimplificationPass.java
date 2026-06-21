package liquidjava.rj_language.opt;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;

/**
 * Base implementation for passes that simplify one refinement expression at a time.
 */
abstract class VCExpressionSimplificationPass<C> implements VCSimplificationPass {

    @Override
    public final VCImplication apply(VCImplication implication) {
        return apply(implication, initialContext());
    }

    protected C initialContext() {
        return null;
    }

    protected C nextContext(C context, VCImplication implication) {
        return context;
    }

    protected abstract Expression simplify(Expression expression, C context);

    private VCImplication apply(VCImplication implication, C context) {
        if (implication == null)
            return null;

        Expression expression = implication.getRefinement().getExpression();
        Expression simplified = simplify(expression, context);
        if (!expression.equals(simplified)) {
            VCImplication result = new SimplifiedVCImplication(implication, new Predicate(simplified), implication);
            result.setNext(implication.getNext() == null ? null : implication.getNext().clone());
            return result;
        }

        VCImplication next = apply(implication.getNext(), nextContext(context, implication));
        if (implication.getNext() == null || implication.getNext().equals(next))
            return implication;

        VCImplication result = implication.copyWithRefinement(implication.getRefinement().clone());
        result.setNext(next);
        return result;
    }
}
