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
     * @param shownFound
     *            what the error shows as found: {@code found} with what is known about the values it mentions, such as
     *            the result of an earlier call that checked the receiver's state
     * @param ghostStates
     *            the ghost states of the receiver's class
     *
     * @return the category, or nothing if it cannot be decided
     */
    public static Optional<ErrorCategory> categorize(StateMachine machine, Set<State> current, Predicate found,
            Predicate shownFound, List<GhostState> ghostStates) {
        List<GhostState> states = ghostStates.stream().filter(g -> g.getParent() != null).toList();
        if (current.size() == machine.states().size() && found.getStateInvocations(states).isEmpty()) {
            // all states are possible and none is mentioned: nothing is known, which differs from a join of every
            // state; a state mentioned only through another value, like a check's result, is not a lack of knowledge
            return shownFound.getStateInvocations(states).isEmpty() ? Optional.of(ErrorCategory.UNKNOWN)
                    : Optional.empty();
        }
        if (current.size() == 1)
            return Optional.of(ErrorCategory.OUT_OF_ORDER);
        // with several state sets, the states may differ only in a set the call does not check
        boolean oneStateSet = states.stream().map(GhostState::getParent).distinct().count() == 1;
        if (current.size() > 1 && oneStateSet)
            return Optional.of(ErrorCategory.PATH_DEPENDENT_STATE);
        return Optional.empty();
    }
}
