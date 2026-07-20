package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.copyWithRefinement;
import static liquidjava.rj_language.opt.VCSimplificationUtils.containsExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;

/**
 * Simplifies VCImplication chains by propagating exact function invocation equalities
 */
public class VCFunctionSubstitution implements VCSimplificationPass {

    /**
     * A substitution discovered from a function invocation equality. At {@code sourceNode}, remove
     * {@code sourceEquality} and in the following nodes replace {@code invocation} with {@code replacement}
     */
    private record Substitution(VCImplication sourceNode, FunctionInvocation invocation, Expression replacement,
            Expression sourceEquality) {
    }

    /**
     * Applies one function invocation substitution in a VC chain
     */
    @Override
    public VCImplication apply(VCImplication implication) {
        Optional<Substitution> substitutionOpt = findSubstitution(implication);

        if (substitutionOpt.isPresent()) {
            Substitution substitution = substitutionOpt.get();
            return substitute(implication, substitution.sourceNode(), substitution.invocation(),
                    substitution.replacement(), substitution.sourceEquality());
        }
        return implication;
    }

    /**
     * Rewrites one VC chain with a single substitution and removes its source equality
     */
    private VCImplication substitute(VCImplication implication, VCImplication node, FunctionInvocation invocation,
            Expression replacement, Expression sourceEquality) {
        if (implication == null)
            return null;

        // consume the source equality and start substitution from the next node
        if (implication == node) {
            VCImplication suffix = substituteSuffix(implication.getNext(), invocation, replacement);
            VCImplication source = removeSourceEquality(implication, sourceEquality);
            if (source == null)
                return suffix;
            source.setNext(suffix);
            return source;
        }

        // preserve the current node and continue rewriting the suffix
        VCImplication result = copyWithRefinement(implication, implication.getRefinement());
        result.setNext(substitute(implication.getNext(), node, invocation, replacement, sourceEquality));
        return result;
    }

    /**
     * Removes the equality conjunct that supplied the substitution, preserving any sibling conjuncts
     */
    private VCImplication removeSourceEquality(VCImplication implication, Expression sourceEquality) {
        List<Expression> remaining = new ArrayList<>(implication.getRefinement().getExpression().getConjuncts());
        remaining.remove(sourceEquality);
        if (remaining.isEmpty())
            return null;

        Predicate refinement = new Predicate();
        for (Expression conjunct : remaining)
            refinement = Predicate.createConjunction(refinement, new Predicate(conjunct));
        return copyWithRefinement(implication, refinement);
    }

    /**
     * Rewrites every node after the source equality with one function substitution
     */
    private VCImplication substituteSuffix(VCImplication implication, FunctionInvocation invocation,
            Expression replacement) {
        if (implication == null)
            return null;

        VCImplication result = substituteNode(implication, invocation, replacement);
        result.setNext(substituteSuffix(implication.getNext(), invocation, replacement));
        return result;
    }

    /**
     * Substitutes one exact function invocation inside one VC node
     */
    private VCImplication substituteNode(VCImplication implication, FunctionInvocation invocation,
            Expression replacement) {
        Expression expression = implication.getRefinement().getExpression();
        if (!containsExpression(expression, invocation))
            return copyWithRefinement(implication, implication.getRefinement());

        Expression substituted = expression.substitute(invocation, replacement);
        return copyWithRefinement(implication, new Predicate(substituted));
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
        return getSubstitution(implication, implication.getRefinement().getExpression());
    }

    /**
     * Extracts a substitution from a top-level equality or conjunction
     */
    private Optional<Substitution> getSubstitution(VCImplication implication, Expression expression) {
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
            return Optional.of(new Substitution(implication, invocation, right, binary));
        if (right instanceof FunctionInvocation invocation && !containsExpression(left, right))
            return Optional.of(new Substitution(implication, invocation, left, binary));

        return Optional.empty();
    }

}
