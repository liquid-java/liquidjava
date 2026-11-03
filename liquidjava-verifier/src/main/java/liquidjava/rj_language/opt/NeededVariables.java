package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.isTrue;
import static liquidjava.rj_language.opt.VCSimplificationUtils.linkedConjuncts;
import static liquidjava.rj_language.opt.VCSimplificationUtils.splitConjuncts;
import static liquidjava.rj_language.opt.VCSimplificationUtils.termsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.valueVariablesOf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;

/**
 * Decides which binders refined only by {@code true} a simplified VC chain keeps for an error. Such a binder states no
 * fact, so it is kept only when a fact linked to the expected refinement (sharing a term with it, directly or through
 * other facts) constrains its variable's value, or, when the error shows a counterexample, when expected itself
 * constrains its value, so that the counterexample can show it
 *
 * @param expectedTerms
 *            variables and ghost applications of the expected refinement
 * @param expectedValues
 *            variables whose value the expected refinement constrains
 * @param keepExpected
 *            whether the binders of expectedValues are kept for that alone
 */
public record NeededVariables(Set<String> expectedTerms, Set<String> expectedValues, boolean keepExpected) {

    public static final NeededVariables NONE = new NeededVariables(Set.of(), Set.of(), false);

    public NeededVariables {
        expectedTerms = Set.copyOf(expectedTerms);
        expectedValues = Set.copyOf(expectedValues);
    }

    /**
     * For a refinement error, whose counterexample lists the values of the binders
     */
    public static NeededVariables forRefinementError(Predicate expected) {
        return of(expected, true);
    }

    /**
     * For a state refinement error, which has no counterexample
     */
    public static NeededVariables forStateError(Predicate expected) {
        return of(expected, false);
    }

    private static NeededVariables of(Predicate expected, boolean keepExpected) {
        if (expected == null)
            return new NeededVariables(Set.of(), Set.of(), keepExpected);
        return new NeededVariables(new HashSet<>(termsOf(expected.getExpression())),
                new HashSet<>(valueVariablesOf(expected.getExpression())), keepExpected);
    }

    /**
     * Variables whose binder refined only by true the chain keeps: those whose own value a fact linked to expected
     * constrains. A variable named only as the object of a ghost or state application ({@code x} in
     * {@code b == size(x)}) has no value of its own there, so its row would say nothing
     */
    public Set<String> trueBindersToKeep(VCImplication chain) {
        List<Expression> facts = new ArrayList<>();
        for (VCImplication current = chain; current != null; current = current.getNext())
            if (!isTrue(current.getRefinement().getExpression()))
                facts.addAll(splitConjuncts(current.getRefinement().getExpression()));

        Set<String> keep = new HashSet<>(keepExpected ? expectedValues : Set.of());
        for (Expression fact : linkedConjuncts(facts, VCSimplificationUtils::termsOf, expectedTerms))
            for (String name : valueVariablesOf(fact))
                if (!expectedTerms.contains(name))
                    keep.add(name);
        return keep;
    }
}
