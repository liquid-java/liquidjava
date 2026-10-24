package liquidjava.processor.state_machine;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.processor.context.GhostState;
import liquidjava.rj_language.Predicate;

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
     * @param ghostStates
     *            the ghost states of the receiver's class
     *
     * @return the category, or nothing if it cannot be decided
     */
    public static Optional<ErrorCategory> categorize(StateMachine machine, Set<State> current, Predicate found,
            List<GhostState> ghostStates) {
        List<GhostState> states = ghostStates.stream().filter(g -> g.getParent() != null).toList();
        // all states are possible and none is mentioned: nothing is known, which differs from a join of every state
        if (current.size() == machine.states().size() && found.getStateInvocations(states).isEmpty())
            return Optional.of(ErrorCategory.UNPROVABLE);
        if (current.size() == 1)
            return Optional.of(ErrorCategory.OUT_OF_ORDER);
        // with several state sets, the states may differ only in a set the call does not check
        boolean oneStateSet = states.stream().map(GhostState::getParent).distinct().count() == 1;
        if (current.size() > 1 && oneStateSet)
            return Optional.of(ErrorCategory.PATH_DEPENDENT_STATE);
        return Optional.empty();
    }
}
