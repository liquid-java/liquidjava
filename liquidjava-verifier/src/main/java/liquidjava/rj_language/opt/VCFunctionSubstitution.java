package liquidjava.rj_language.opt;

import java.util.Optional;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.GroupExpression;

/**
 * Simplifies VCImplication chains by propagating exact function invocation equalities
 */
public class VCFunctionSubstitution implements VCSimplificationPass {

    /**
     * A substitution discovered from a function invocation equality
     */
    private record Substitution(VCImplication node, FunctionInvocation invocation, Expression replacement) {
    }

    /**
     * Applies one function invocation substitution in a VC chain
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        VCImplication result = implication.clone();
        Optional<Substitution> substitutionOpt = findSubstitution(result);

        if (substitutionOpt.isPresent()) {
            Substitution substitution = substitutionOpt.get();
            result = substitute(result, substitution.node(), substitution.invocation(), substitution.replacement());
        }
        return result;
    }

    /**
     * Preserves nodes before the source equality and starts rewriting at the source suffix
     */
    private VCImplication substitute(VCImplication implication, VCImplication node, FunctionInvocation invocation,
            Expression replacement) {
        if (implication == null)
            return null;

        // skip the source node to remove it from the chain and start substitution from the next node
        if (implication == node) {
            VCImplication result = implication.copyWithRefinement(implication.getRefinement().clone());
            result.setNext(substituteSuffix(implication.getNext(), node, invocation, replacement));
            return result;
        }

        // preserve the current node and continue rewriting the suffix
        VCImplication result = implication.copyWithRefinement(implication.getRefinement().clone());
        result.setNext(substitute(implication.getNext(), node, invocation, replacement));
        return result;
    }

    /**
     * Rewrites every node after the source equality with one function substitution
     */
    private VCImplication substituteSuffix(VCImplication implication, VCImplication source,
            FunctionInvocation invocation, Expression replacement) {
        if (implication == null)
            return null;

        VCImplication result = substituteNode(implication, source, invocation, replacement);
        result.setNext(substituteSuffix(implication.getNext(), source, invocation, replacement));
        return result;
    }

    /**
     * Substitutes one exact function invocation inside one VC node while preserving simplification metadata
     */
    private VCImplication substituteNode(VCImplication implication, VCImplication source, FunctionInvocation invocation,
            Expression replacement) {
        Expression expression = implication.getRefinement().getExpression().clone();
        if (!containsExpression(expression, invocation))
            return implication.copyWithRefinement(new Predicate(expression));

        Expression substituted = expression.substitute(invocation, replacement.clone());
        return new SimplifiedVCImplication(implication, new Predicate(substituted), source);
    }

    /**
     * Finds the first function substitution candidate that is used in the remaining suffix
     */
    private Optional<Substitution> findSubstitution(VCImplication implication) {
        if (implication == null)
            return Optional.empty();

        Optional<Substitution> current = getSubstitution(implication);
        if (current.isPresent() && containsExpression(implication.getNext(), current.get().invocation()))
            return current;

        return findSubstitution(implication.getNext());
    }

    /**
     * Extracts a substitution from one VC node refinement
     */
    private Optional<Substitution> getSubstitution(VCImplication implication) {
        return getSubstitution(implication, implication.getRefinement().getExpression().clone());
    }

    /**
     * Extracts a substitution from a top-level equality or conjunction
     */
    private Optional<Substitution> getSubstitution(VCImplication implication, Expression expression) {
        if (expression instanceof GroupExpression group)
            return getSubstitution(implication, group.getExpression());

        if (expression instanceof BinaryExpression binary && "&&".equals(binary.getOperator())) {
            Optional<Substitution> left = getSubstitution(implication, binary.getFirstOperand());
            if (left.isPresent())
                return left;
            return getSubstitution(implication, binary.getSecondOperand());
        }

        if (!(expression instanceof BinaryExpression binary) || !"==".equals(binary.getOperator()))
            return Optional.empty();

        Expression left = binary.getFirstOperand();
        Expression right = binary.getSecondOperand();
        if (left instanceof FunctionInvocation invocation && !containsExpression(right, left))
            return Optional.of(new Substitution(implication, (FunctionInvocation) invocation.clone(), right.clone()));
        if (right instanceof FunctionInvocation invocation && !containsExpression(left, right))
            return Optional.of(new Substitution(implication, (FunctionInvocation) invocation.clone(), left.clone()));

        return Optional.empty();
    }

    /**
     * Checks whether an expression contains another expression
     */
    private boolean containsExpression(Expression expression, Expression target) {
        if (expression.equals(target))
            return true;

        for (Expression child : expression.getChildren())
            if (containsExpression(child, target))
                return true;
        return false;
    }

    /**
     * Checks whether a VC suffix contains an expression
     */
    private boolean containsExpression(VCImplication implication, Expression target) {
        for (VCImplication current = implication; current != null; current = current.getNext())
            if (containsExpression(current.getRefinement().getExpression(), target))
                return true;
        return false;
    }
}
