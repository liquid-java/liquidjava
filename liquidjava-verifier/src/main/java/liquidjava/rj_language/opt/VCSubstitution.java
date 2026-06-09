package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import liquidjava.processor.SimplifiedVCImplication;
import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Var;

/**
 * Simplifies VCImplication chains by replacing binder equalities with their known values
 */
public class VCSubstitution {

    /**
     * A substitution discovered from an implication node
     */
    private record Substitution(VCImplication node, Expression replacement) {
    }

    /**
     * Applies one substitution in a VC chain
     */
    public static VCImplication apply(VCImplication implication) {
        if (implication == null)
            return null;

        VCImplication result = implication.clone();
        Optional<VCSubstitution.Substitution> substitutionOpt = VCSubstitution.findSubstitution(result);

        // apply only the first available substitution
        if (substitutionOpt.isPresent()) {
            VCSubstitution.Substitution substitution = substitutionOpt.get();
            result = VCSubstitution.substitute(result, substitution.node(), substitution.replacement());
        }
        return result;
    }

    /**
     * Rewrites one VC chain with a single substitution and removes its source node
     */
    private static VCImplication substitute(VCImplication implication, VCImplication node, Expression replacement) {
        if (implication == null)
            return null;

        // skip the source node to remove it from the chain and start substitution from the next node
        if (implication == node)
            return substitute(implication.getNext(), node, replacement);

        VCImplication result = substituteNode(implication, node, replacement);
        result.setNext(substitute(implication.getNext(), node, replacement));
        return result;
    }

    /**
     * Substitutes a source binder inside one VC node while preserving simplification metadata
     */
    private static VCImplication substituteNode(VCImplication implication, VCImplication node, Expression replacement) {
        Expression exp = implication.getRefinement().getExpression().clone();
        if (!containsVar(exp, node.getName()))
            return implication.copyWithRefinement(new Predicate(exp));

        Expression substituted = exp.substitute(new Var(node.getName()), replacement.clone());
        VCImplication origin = new VCImplication(node.getName(), node.getType(), implication.getOriginRefinement());
        return new SimplifiedVCImplication(implication, new Predicate(substituted), origin);
    }

    /**
     * Finds the first substitution candidate in the VC chain
     */
    private static Optional<Substitution> findSubstitution(VCImplication implication) {
        if (implication == null)
            return Optional.empty();

        Optional<Substitution> current = getSubstitution(implication);
        if (current.isPresent())
            return current;

        return findSubstitution(implication.getNext());
    }

    /**
     * Extracts a substitution from one binder equality
     */
    private static Optional<Substitution> getSubstitution(VCImplication implication) {
        if (!implication.hasBinder())
            return Optional.empty();

        Expression refinement = implication.getRefinement().getExpression().clone();
        if (!(refinement instanceof BinaryExpression binary) || !"==".equals(binary.getOperator()))
            return Optional.empty();

        String name = implication.getName();
        Expression left = binary.getFirstOperand();
        Expression right = binary.getSecondOperand();

        if (isVar(left, name) && !containsVar(right, name))
            return Optional.of(new Substitution(implication, right.clone()));
        if (isVar(right, name) && !containsVar(left, name))
            return Optional.of(new Substitution(implication, left.clone()));

        return Optional.empty();
    }

    /**
     * Checks whether an expression is a variable with a given name
     */
    public static boolean isVar(Expression expression, String name) {
        return expression instanceof Var var && name.equals(var.getName());
    }

    /**
     * Checks whether an expression contains a variable name
     */
    public static boolean containsVar(Expression expression, String name) {
        List<String> names = new ArrayList<>();
        expression.getVariableNames(names);
        return names.contains(name);
    }
}
