package liquidjava.rj_language.opt;

import java.util.HashSet;
import java.util.Set;

import liquidjava.rj_language.Predicate;
import liquidjava.smt.Counterexample;
import liquidjava.utils.Pair;

/**
 * Variables that a simplified VC chain must keep a binder for even when the binder only says {@code true}: those of the
 * expected refinement, and those that the counterexample gives a value
 */
public record NeededVariables(Set<String> expected, Set<String> assigned) {

    public static final NeededVariables NONE = new NeededVariables(Set.of(), Set.of());

    public NeededVariables {
        expected = Set.copyOf(expected);
        assigned = Set.copyOf(assigned);
    }

    public static NeededVariables of(Predicate expected, Counterexample counterexample) {
        Set<String> expectedNames = expected == null ? Set.of() : new HashSet<>(expected.getVariableNames());
        Set<String> assignedNames = new HashSet<>();
        if (counterexample != null)
            counterexample.assignments().stream().map(Pair::first).forEach(assignedNames::add);
        return new NeededVariables(expectedNames, assignedNames);
    }

    /**
     * Whether a binder refined only by {@code true} must stay: the expected refinement is about its variable, or a
     * later implication uses it and the counterexample has a value for it
     */
    public boolean needsTrueBinder(String name, boolean usedDownstream) {
        return expected.contains(name) || usedDownstream && assigned.contains(name);
    }
}
