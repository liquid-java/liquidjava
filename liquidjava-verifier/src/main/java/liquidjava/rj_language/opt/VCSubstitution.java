package liquidjava.rj_language.opt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.SimplifiedPredicate;
import liquidjava.rj_language.SimplifiedPredicate.Binder;
import liquidjava.rj_language.ast.BinaryExpression;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.Var;

import static liquidjava.rj_language.opt.VCSimplificationUtils.*;

/**
 * Simplifies VCImplication chains by replacing binder equalities with their known values
 */
public class VCSubstitution {

    /**
     * A substitution discovered from an implication node
     */
    private record Substitution(VCImplication source, Expression value) {
    }

    /**
     * Applies one substitution in a VC chain
     */
    public static VCImplication applyOnce(VCImplication implication) {
        if (implication == null)
            return null;

        VCImplication result = implication.clone();
        Optional<VCSubstitution.Substitution> substitutionOpt = VCSubstitution.findSubstitution(result);

        // apply only the first available substitution
        if (substitutionOpt.isPresent()) {
            VCSubstitution.Substitution substitution = substitutionOpt.get();
            result = VCSubstitution.substitute(result, substitution.source(), substitution.value());
        }
        return result;
    }

    /**
     * Rewrites one VC chain with a single substitution and removes its source node
     */
    private static VCImplication substitute(VCImplication implication, VCImplication source, Expression value) {
        if (implication == null)
            return null;

        // skip the source node to remove it from the chain and start substitution from the next node
        if (implication == source)
            return substitute(implication.getNext(), source, value);

        Predicate refinement = substituteRefinement(implication.getRefinement(), source, value);
        VCImplication result = VCSimplificationUtils.copyWithRefinement(implication, refinement);
        result.setNext(substitute(implication.getNext(), source, value));
        return result;
    }

    /**
     * Substitutes a source binder inside one predicate while preserving simplification metadata
     */
    private static Predicate substituteRefinement(Predicate refinement, VCImplication source, Expression value) {
        Expression active = activeExpression(refinement);
        Binder binder = new Binder(source.getName(), source.getType());
        Expression substituted = active.substitute(new Var(binder.getName()), value.clone());

        return new SimplifiedPredicate(new Predicate(substituted), VCSimplificationUtils.originPredicate(refinement),
                bindersAfterSubstitution(refinement, active, binder));
    }

    /**
     * Builds the binder metadata after one substitution
     */
    private static List<Binder> bindersAfterSubstitution(Predicate refinement, Expression active,
            SimplifiedPredicate.Binder binder) {
        List<SimplifiedPredicate.Binder> binders = refinement instanceof SimplifiedPredicate previous
                ? new ArrayList<>(previous.getBinders()) : new ArrayList<>();
        if (containsVariable(active, binder.getName()) && !binders.contains(binder))
            binders.add(binder);
        return binders;
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

        Expression refinement = activeExpression(implication.getRefinement());
        if (!(refinement instanceof BinaryExpression binary) || !"==".equals(binary.getOperator()))
            return Optional.empty();

        String name = implication.getName();
        Expression left = binary.getFirstOperand();
        Expression right = binary.getSecondOperand();

        if (isVar(left, name) && !containsVariable(right, name))
            return Optional.of(new Substitution(implication, right.clone()));
        if (isVar(right, name) && !containsVariable(left, name))
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
    public static boolean containsVariable(Expression expression, String name) {
        List<String> names = new ArrayList<>();
        expression.getVariableNames(names);
        return names.contains(name);
    }
}
