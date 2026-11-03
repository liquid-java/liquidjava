package liquidjava.rj_language.opt;

import static liquidjava.rj_language.opt.VCSimplificationUtils.containsVar;
import static liquidjava.rj_language.opt.VCSimplificationUtils.termsOf;
import static liquidjava.rj_language.opt.VCSimplificationUtils.valueVariablesOf;

import java.util.HashSet;
import java.util.Set;

import liquidjava.processor.VCImplication;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;

/**
 * Decides which binders refined only by {@code true} a simplified VC chain keeps for an error. Such a binder states no
 * fact, and a variable that another row mentions is already shown there, so its row would only read as if its value
 * were true. It is kept only when the error shows a counterexample, expected constrains the variable's value and no
 * other row mentions it: the row then shows that nothing is known about a value the error depends on
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

    private static Set<String> primitiveBinders(VCImplication chain) {
        Set<String> primitives = new HashSet<>();
        for (VCImplication current = chain; current != null; current = current.getNext())
            if (current.hasBinder() && current.getType().isPrimitive())
                primitives.add(current.getName());
        return primitives;
    }
}
