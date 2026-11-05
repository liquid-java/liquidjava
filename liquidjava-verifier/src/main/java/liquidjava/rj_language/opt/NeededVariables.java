package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.containsVar;
import static liquidjava.rj_language.opt.VCSimplificationUtils.factsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.linkedTerms;
import static liquidjava.rj_language.opt.VCSimplificationUtils.termsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.valueVariablesOf;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;

/**
 * Decides which binders refined only by {@code true} a simplified VC chain keeps for an error. Such a binder states no
 * fact, and a variable that another row mentions is already shown there, so its row would only read as if its value
 * were true. It is kept only when the error shows a counterexample, expected constrains the variable's value and no
 * other row mentions it: the row then shows that nothing is known about a value the error depends on. It also tells
 * which binders expected does not depend on, such as path conditions over other values
 *
 * @param expectedTerms
 *            variables and ghost applications of the expected refinement
 * @param expectedValues
 *            variables whose value the expected refinement constrains
 * @param keepExpected
 *            whether the binders of expectedValues that no other row mentions are kept
 */
public record NeededVariables(Set<String> expectedTerms, Set<String> expectedValues, boolean keepExpected) {

    public static final NeededVariables NONE = new NeededVariables(Set.of(), Set.of(), false);

    public NeededVariables {
        expectedTerms = Set.copyOf(expectedTerms);
        expectedValues = Set.copyOf(expectedValues);
    }

    /**
     * For a refinement error, which shows a counterexample
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
        Expression expression = expected.getExpression();
        return new NeededVariables(new HashSet<>(termsOf(expression)), new HashSet<>(valueVariablesOf(expression)),
                keepExpected);
    }

    /**
     * Variables whose binder refined only by true the chain keeps: when {@code keepExpected}, those whose value
     * expected constrains and that no other row of the chain mentions
     */
    public Set<String> trueBindersToKeep(VCImplication chain) {
        Set<String> keep = new HashSet<>();
        if (!keepExpected)
            return keep;
        Set<String> primitives = primitiveBinders(chain);
        for (String name : expectedTerms)
            if ((expectedValues.contains(name) || primitives.contains(name)) && !containsVar(chain, name))
                keep.add(name);
        return keep;
    }

    /**
     * Binders that share no term with expected, neither their variable nor a term of their refinement, directly or
     * through other facts of the chain, and whose variable no other row mentions: expected does not depend on them.
     * None without expected terms
     */
    public Set<String> unlinkedBinders(VCImplication chain) {
        Set<String> unlinked = new HashSet<>();
        if (expectedTerms.isEmpty())
            return unlinked;
        List<Expression> facts = factsOf(chain.toPredicate().getExpression());
        Set<String> linked = linkedTerms(facts, expectedTerms);
        for (VCImplication row = chain; row != null; row = row.getNext())
            if (row.hasBinder() && !isLinked(row, linked) && !mentionedByOtherRows(chain, row))
                unlinked.add(row.getName());
        return unlinked;
    }

    private static boolean isLinked(VCImplication row, Set<String> linked) {
        return linked.contains(row.getName())
                || termsOf(row.getRefinement().getExpression()).stream().anyMatch(linked::contains);
    }

    private static boolean mentionedByOtherRows(VCImplication chain, VCImplication row) {
        for (VCImplication other = chain; other != null; other = other.getNext())
            if (other != row && containsVar(other.getRefinement().getExpression(), row.getName()))
                return true;
        return false;
    }

    private static Set<String> primitiveBinders(VCImplication chain) {
        Set<String> primitives = new HashSet<>();
        for (VCImplication current = chain; current != null; current = current.getNext())
            if (current.hasBinder() && current.getType().isPrimitive())
                primitives.add(current.getName());
        return primitives;
    }
}
