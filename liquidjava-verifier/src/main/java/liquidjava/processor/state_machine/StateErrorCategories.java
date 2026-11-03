package liquidjava.processor.state_machine;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.GhostState;
import liquidjava.rj_language.Predicate;
import liquidjava.rj_language.ast.Expression;
import liquidjava.rj_language.ast.FunctionInvocation;
import liquidjava.rj_language.ast.Var;

/**
 * The {@link ErrorCategory} of a call made in a state that does not allow it
 */
public final class StateErrorCategories {

    private StateErrorCategories() {
    }

    /**
     * @param current
     *            the states the receiver can be in before the call
     * @param found
     *            what is known about the receiver before the call
     * @param checkedElsewhere
     *            whether its state was only checked elsewhere, as told by {@link #checkedElsewhere}
     * @param ghostStates
     *            the ghost states of the receiver's class
     *
     * @return the category, or nothing if it cannot be decided
     */
    public static Optional<ErrorCategory> categorize(StateMachine machine, Set<State> current, Predicate found,
            boolean checkedElsewhere, List<GhostState> ghostStates) {
        List<GhostState> states = stateGhosts(ghostStates);
        // nothing is known, which differs from a join of every state and from a check that did not decide the state
        if (nothingMentioned(machine, current, found, states))
            return checkedElsewhere ? Optional.empty() : Optional.of(ErrorCategory.UNKNOWN);
        if (current.size() == 1)
            return Optional.of(ErrorCategory.OUT_OF_ORDER);
        // with several state sets, the states may differ only in a set the call does not check
        boolean oneStateSet = states.stream().map(GhostState::getParent).distinct().count() == 1;
        if (current.size() > 1 && oneStateSet)
            return Optional.of(ErrorCategory.PATH_DEPENDENT_STATE);
        return Optional.empty();
    }

    /**
     * Whether every state is possible and {@code found} mentions none of them, though what is known about the values it
     * mentions does mention the state of the receiver or of an object equal to it, as the result of an earlier check
     * does. Such a state was checked, but the check did not decide it.
     *
     * @param shownFound
     *            what the error shows as found: {@code found} with what is known about the values it mentions; only
     *            computed when needed
     * @param instance
     *            the receiver's instance before the call
     */
    public static boolean checkedElsewhere(StateMachine machine, Set<State> current, Predicate found,
            Supplier<Predicate> shownFound, String instance, List<GhostState> ghostStates, StateSolver solver)
            throws LJError {
        List<GhostState> states = stateGhosts(ghostStates);
        if (!nothingMentioned(machine, current, found, states))
            return false;
        Predicate shown = shownFound.get();
        Predicate receiver = Predicate.createVar(instance);
        for (String object : objectsWithState(shown.getExpression(), states, new LinkedHashSet<>()))
            if (object.equals(instance)
                    || solver.implies(shown, Predicate.createEquals(Predicate.createVar(object), receiver)))
                return true;
        return false;
    }

    /** The variables that {@code expression} applies one of {@code states} to */
    private static Set<String> objectsWithState(Expression expression, List<GhostState> states, Set<String> objects) {
        if (expression instanceof FunctionInvocation invocation
                && states.stream().anyMatch(g -> g.matches(invocation.getName())))
            invocation.getArgs().stream().filter(Var.class::isInstance).map(v -> ((Var) v).getName())
                    .forEach(objects::add);
        expression.getChildren().forEach(child -> objectsWithState(child, states, objects));
        return objects;
    }

    /** Whether every state is possible and {@code found} mentions none of them */
    private static boolean nothingMentioned(StateMachine machine, Set<State> current, Predicate found,
            List<GhostState> states) {
        return current.size() == machine.states().size() && found.getStateInvocations(states).isEmpty();
    }

    private static List<GhostState> stateGhosts(List<GhostState> ghostStates) {
        return ghostStates.stream().filter(g -> g.getParent() != null).toList();
    }
}
