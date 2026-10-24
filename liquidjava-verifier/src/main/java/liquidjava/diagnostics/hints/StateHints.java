package liquidjava.diagnostics.hints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint.Kind;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.context.Variable;
import liquidjava.processor.state_machine.DecidingArgument;
import liquidjava.processor.state_machine.State;
import liquidjava.processor.state_machine.StateMachine;
import liquidjava.processor.state_machine.Transition;
import liquidjava.utils.Utils;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Hints for a call made in a state that does not allow it, read from the class's {@link StateMachine}. The error's
 * category decides which kinds of hints are given, and each hint only states what the machine proves, so a suggested
 * sequence of calls always makes the failing call verify.
 */
public final class StateHints {

    private static final int MAX_SEQUENCE_LENGTH = 3;
    private static final int MAX_SEQUENCES = 3;
    private static final int MAX_LISTED_CALLS = 5;

    private final StateMachine machine;
    private final String receiver;

    private StateHints(StateMachine machine, String receiver) {
        this.machine = machine;
        this.receiver = receiver;
    }

    /**
     * @param call
     *            the method that cannot be called
     * @param current
     *            the states the receiver can be in before the call
     * @param receiver
     *            the receiver as written in the code
     * @param category
     *            the error's category, or {@code null} if it has none
     */
    public static List<Hint> forCall(StateMachine machine, RefinedFunction call, Set<State> current, String receiver,
            ErrorCategory category) {
        if (!machine.models(call) || current.isEmpty() || machine.allows(current, call))
            return List.of(); // the machine cannot explain why the call failed
        Set<State> goal = machine.statesAllowing(call);
        if (goal.isEmpty())
            return List.of();

        StateHints hints = new StateHints(machine, receiver);
        if (category == ErrorCategory.UNPROVABLE)
            return hints.reachingCalls(call, goal);
        boolean pathDependent = category == ErrorCategory.PATH_DEPENDENT_STATE;
        return Stream.concat(hints.howToReach(current, call, goal, pathDependent).stream(),
                hints.allowedCalls(current).stream()).toList();
    }

    /** The earlier call whose arguments decided the receiver's state, and values that would allow the call */
    public static Hint decidingArgument(DecidingArgument deciding, String receiver) {
        RefinedFunction function = deciding.call().function();
        String call = CtExecutableReference.CONSTRUCTOR_NAME.equals(function.getName()) ? constructor(function)
                : receiver + "." + signature(function);
        String values = deciding.values().isEmpty() ? "other arguments" : deciding.values().entrySet().stream()
                .map(e -> e.getKey() + " == " + e.getValue()).collect(Collectors.joining(" and "));
        return new Hint(Kind.DECIDING_ARGUMENT,
                String.format("%s on line %d decides the state of %s: with %s, this call would be allowed", call,
                        deciding.call().position().getLine(), receiver, values));
    }

    /** Nothing is known about the receiver's state: list what leads to a state where {@code call} is allowed */
    private List<Hint> reachingCalls(RefinedFunction call, Set<State> goal) {
        Map<RefinedFunction, List<State>> sources = new LinkedHashMap<>();
        for (Transition t : machine.transitionsEnabling(call))
            sources.computeIfAbsent(t.method(), m -> new ArrayList<>()).add(t.from());
        Stream<String> constructors = machine.constructorsEnabling(call).stream().map(StateHints::constructor);
        Stream<String> calls = sources.entrySet().stream()
                .map(e -> call(e.getKey()) + " (from " + names(e.getValue()) + ")");
        List<String> ways = Stream.concat(constructors, calls).toList();
        if (ways.isEmpty())
            return List.of();
        return List.of(new Hint(Kind.REACHING_CALLS, String.format(
                "The state of %s is unknown here. To get it to %s, use: %s", receiver, names(goal), list(ways))));
    }

    private List<Hint> howToReach(Set<State> current, RefinedFunction call, Set<State> goal, boolean pathDependent) {
        Optional<List<List<RefinedFunction>>> search = machine.shortestPaths(current, call, MAX_SEQUENCES);
        if (search.isEmpty())
            return List.of();
        List<List<RefinedFunction>> sequences = search.get();
        if (sequences.isEmpty())
            return machine.complete() ? List.of(unreachable(current, call, goal, pathDependent)) : List.of();
        if (sequences.get(0).size() > MAX_SEQUENCE_LENGTH)
            return List.of();
        return sequences.stream().map(sequence -> callSequence(current, sequence)).toList();
    }

    private Hint callSequence(Set<State> current, List<RefinedFunction> sequence) {
        Set<State> reached = current;
        for (RefinedFunction method : sequence)
            reached = machine.after(reached, method);
        String calls = sequence.stream().map(this::call).collect(Collectors.joining(", then "));
        return new Hint(Kind.CALL_SEQUENCE, String.format("Call %s before this call to move %s from %s to %s", calls,
                receiver, names(current), names(reached)));
    }

    private Hint unreachable(Set<State> current, RefinedFunction call, Set<State> goal, boolean pathDependent) {
        if (pathDependent)
            return new Hint(Kind.PATH_DEPENDENT_STATE, String.format(
                    "%s can be in %s here, depending on the path taken, and no sequence of calls moves all of them to %s. "
                            + "Make %s reach this line in the same state on every path",
                    receiver, names(current), names(goal), receiver));
        String message = String.format("No sequence of calls moves %s from %s to %s", receiver, names(current),
                names(goal));
        List<String> constructors = machine.constructorsEnabling(call).stream().map(StateHints::constructor).toList();
        if (!constructors.isEmpty())
            message += ". Create a new object instead with " + String.join(" or ", constructors);
        return new Hint(Kind.UNREACHABLE_STATE, message);
    }

    private List<Hint> allowedCalls(Set<State> current) {
        List<RefinedFunction> allowed = machine.allowedIn(current);
        if (!allowed.isEmpty()) {
            List<String> signatures = allowed.stream().map(StateHints::signature).toList();
            String others = machine.complete() ? "" : " (and possibly others)";
            return List.of(new Hint(Kind.AVAILABLE_CALLS, String.format("In state %s, %s can call: %s%s",
                    names(current), receiver, list(signatures), others)));
        }
        boolean noCallsInAnyState = current.stream().allMatch(s -> machine.allowedIn(Set.of(s)).isEmpty());
        if (machine.complete() && noCallsInAnyState)
            return List.of(new Hint(Kind.FINAL_STATE,
                    String.format("%s is in state %s, where none of its methods with state refinements can be called",
                            receiver, names(current))));
        return List.of();
    }

    private String call(RefinedFunction method) {
        return receiver + "." + signature(method);
    }

    private static String signature(RefinedFunction method) {
        return Utils.getSimpleName(method.getName()) + parameters(method);
    }

    private static String constructor(RefinedFunction constructor) {
        return "new " + Utils.getSimpleName(constructor.getTargetClass()) + parameters(constructor);
    }

    private static String parameters(RefinedFunction function) {
        return function.getArguments().stream().map(Variable::getType).map(CtTypeReference::getSimpleName)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String names(Collection<State> states) {
        return states.stream().map(State::name).collect(Collectors.joining(" or "));
    }

    private static String list(List<String> items) {
        if (items.size() <= MAX_LISTED_CALLS)
            return String.join(", ", items);
        return String.join(", ", items.subList(0, MAX_LISTED_CALLS)) + ", ...";
    }
}
