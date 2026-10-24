package liquidjava.diagnostics.errors;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import liquidjava.diagnostics.hints.StateHints;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.state_machine.State;
import liquidjava.processor.state_machine.StateMachine;

/**
 * What the verifier knows about a call made in a state that does not allow it, for clients that show it. Each state is
 * given by its name in each state set of the class, in the order the sets are declared.
 *
 * @param receiver
 *            the receiver of the call as written in the code
 * @param receiverClass
 *            the qualified name of the receiver's class
 * @param method
 *            the called method, e.g. {@code open(int)}
 * @param possibleStates
 *            the states the receiver can be in before the call
 * @param acceptedStates
 *            the states that allow the call, or null if they depend on more than the receiver's state
 * @param complete
 *            whether every method that changes the receiver's state is known, so a state that no call reaches is
 *            unreachable
 */
public record StateErrorContext(String receiver, String receiverClass, String method, List<List<String>> possibleStates,
        List<List<String>> acceptedStates, boolean complete) {

    public static StateErrorContext of(StateMachine machine, RefinedFunction call, Set<State> current,
            String receiver) {
        List<List<String>> accepted = machine.models(call) ? names(machine.statesAllowing(call)) : null;
        return new StateErrorContext(receiver, call.getTargetClass(), StateHints.signature(call), names(current),
                accepted, machine.complete());
    }

    private static List<List<String>> names(Collection<State> states) {
        return states.stream().map(State::names).toList();
    }
}
