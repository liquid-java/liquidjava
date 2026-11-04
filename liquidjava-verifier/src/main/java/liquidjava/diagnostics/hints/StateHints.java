package liquidjava.diagnostics.hints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import liquidjava.diagnostics.categories.ErrorCategory;
import liquidjava.diagnostics.hints.Hint.Kind;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.context.Variable;
import liquidjava.processor.state_machine.CallVariant;
import liquidjava.processor.state_machine.DecidingArgument;
import liquidjava.processor.state_machine.State;
import liquidjava.processor.state_machine.StateMachine;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Hints for a call made in a state that does not allow it, read from the class's {@link StateMachine}. The error's
 * category decides which kinds of hints are given, and each hint only states what the machine proves, so a suggested
 * sequence of calls, or check of a call's result, always makes the failing call verify.
 */
public final class StateHints {

    private static final int MAX_SEQUENCE_LENGTH = 3;
    private static final int MAX_SEQUENCES = 3;
    private static final int MAX_LISTED_CALLS = 5;

    private final StateMachine machine;
    private final String receiver;
    /** The states that allow the failing call, grouped by the state refinement that allows it */
    private final List<Set<State>> goals;
    /** The state sets the failing call checks, as indexes into {@link State#names()} */
    private final Set<Integer> checkedSets;

    private StateHints(StateMachine machine, String receiver, RefinedFunction call) {
        this.machine = machine;
        this.receiver = receiver;
        this.goals = machine.statesAllowingByRefinement(call);
        this.checkedSets = goals.stream().flatMap(g -> machine.stateSetsNamed(g).stream())
                .collect(Collectors.toCollection(TreeSet::new));
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
     * @param checkedElsewhere
     *            whether the receiver's state was checked elsewhere, though not decided, e.g. by an earlier call whose
     *            result says which state it is in
     */
    public static List<Hint> forCall(StateMachine machine, RefinedFunction call, Set<State> current, String receiver,
            ErrorCategory category, boolean checkedElsewhere) {
        if (!machine.models(call) || current.isEmpty() || machine.allows(current, call))
            return List.of(); // the machine cannot explain why the call failed
        StateHints hints = new StateHints(machine, receiver, call);
        if (hints.goals.isEmpty())
            return List.of();

        if (category == ErrorCategory.UNKNOWN || checkedElsewhere)
            return hints.anyState(current, call, checkedElsewhere);
        boolean pathDependent = category == ErrorCategory.PATH_DEPENDENT_STATE;
        List<Hint> sequences = hints.howToReach(current, call, hints.namesHere(current),
                () -> machine.complete() ? List.of(hints.unreachable(current, call, pathDependent)) : List.of());
        // in a known state, a check is suggested only when no sequence of calls reaches a state that allows the call
        boolean reachable = sequences.stream().anyMatch(h -> h.kind() == Kind.CALL_SEQUENCE);
        List<Hint> guards = reachable && current.size() == 1 ? List.of() : hints.guard(current, call);
        return Stream.of(sequences, guards, hints.allowedCalls(current)).flatMap(List::stream).toList();
    }

    /** The earlier call whose arguments decided the receiver's state, and values that would allow the call */
    public static Hint decidingArgument(DecidingArgument deciding, String receiver) {
        RefinedFunction function = deciding.call().function();
        // the values name the parameters, so the call shows their names
        String call = CtExecutableReference.CONSTRUCTOR_NAME.equals(function.getName())
                ? "new " + Utils.getSimpleName(function.getTargetClass()) + namedParameters(function)
                : receiver + "." + declaration(function);
        String values = deciding.values().isEmpty() ? "other arguments" : deciding.values().entrySet().stream()
                .map(e -> e.getKey() + " == " + e.getValue()).collect(Collectors.joining(" and "));
        return new Hint(Kind.DECIDING_ARGUMENT,
                String.format("%s on line %d decides the state of %s: with %s, this call would be allowed", call,
                        deciding.call().position().getLine(), receiver, values));
    }

    /**
     * The receiver can be in any state: name the state {@code call} needs, the call whose result tells whether the
     * receiver is in it, and the calls that get there from any state. A call allowed in only some states is never
     * suggested, since whether it is allowed is just as unknown.
     *
     * @param checkedElsewhere
     *            whether the state was checked, though the check did not decide it
     */
    private List<Hint> anyState(Set<State> current, RefinedFunction call, boolean checkedElsewhere) {
        String known = checkedElsewhere ? "What is known here does not decide the state of " + receiver
                : "The state of " + receiver + " is unknown here";
        String message = known + ", and this call needs it to be " + needed();
        message += newObjects(call).map(c -> ". Objects created with " + c + " start in such a state").orElse("");
        List<Hint> hints = new ArrayList<>();
        hints.add(new Hint(Kind.NEEDED_STATE, message));
        hints.addAll(guard(current, call));
        hints.addAll(howToReach(current, call, "any state", List::of));
        return hints;
    }

    /**
     * The shortest sequences of calls that are allowed in every state of {@code current} and lead to {@code call}
     *
     * @param from
     *            how to name {@code current}
     * @param unreachable
     *            the hints to give if no sequence exists
     */
    private List<Hint> howToReach(Set<State> current, RefinedFunction call, String from,
            Supplier<List<Hint>> unreachable) {
        Optional<List<List<CallVariant>>> search = machine.shortestPaths(current, call, MAX_SEQUENCES);
        if (search.isEmpty())
            return List.of();
        List<List<CallVariant>> sequences = search.get();
        if (sequences.isEmpty())
            return unreachable.get();
        if (sequences.get(0).size() > MAX_SEQUENCE_LENGTH)
            return List.of();
        return sequences.stream().map(sequence -> callSequence(current, sequence, from)).toList();
    }

    /**
     * The call whose result, checked right before {@code call}, puts the receiver in a state that allows it: one the
     * verifier allows in every state of {@code current}, whose result can have the value checked. If several calls
     * would do, none is suggested, since the refinements do not tell them apart.
     */
    private List<Hint> guard(Set<State> current, RefinedFunction call) {
        List<CallVariant> guards = new ArrayList<>();
        boolean anyCombination = anyCombination(current);
        for (CallVariant guard : machine.guards()) {
            if (anyCombination && !namesCheckedState(guard.method()))
                continue;
            if (!machine.allows(current, guard))
                continue;
            Set<State> reached = machine.after(current, guard);
            if (!reached.isEmpty() && machine.allows(reached, call))
                guards.add(guard);
        }
        if (guards.size() != 1)
            return List.of();
        CallVariant guard = guards.get(0);
        boolean negated = guard.negatedResult();
        String check = (negated ? "!" : "") + guardCall(guard.method());
        String arguments = guard.method().getArguments().isEmpty() ? ""
                : ", filling in the arguments of " + declaration(guard.method());
        String message = String.format("Guard this call with if (%s)%s: when it returns %s, %s is %s", check, arguments,
                !negated, receiver, needed());
        return List.of(new Hint(Kind.GUARD, message));
    }

    /**
     * Whether the receiver can be in any combination of the states {@code current} has in each state set. Then a call
     * that names no state of the sets the failing call checks leaves them as they were, whatever it returns, so the
     * failing call stays disallowed after it
     */
    private boolean anyCombination(Set<State> current) {
        long combinations = 1;
        for (int set = 0; set < current.iterator().next().names().size(); set++) {
            int index = set;
            combinations *= current.stream().map(s -> s.names().get(index)).distinct().count();
        }
        return combinations == current.size();
    }

    /**
     * Whether a state refinement or the return refinement of {@code method} names a state of a set the failing call
     * checks, or an alias
     */
    private boolean namesCheckedState(RefinedFunction method) {
        Set<String> checked = machine.states().stream().flatMap(s -> checkedSets.stream().map(s.names()::get))
                .collect(Collectors.toSet());
        Stream<Predicate> refinements = Stream.concat(
                method.getAllStates().stream().flatMap(o -> Stream.of(o.getFrom(), o.getTo())),
                Stream.ofNullable(method.getRefReturn()));
        return refinements.anyMatch(p -> StateMachine.mayName(p, checked));
    }

    private Hint callSequence(Set<State> current, List<CallVariant> sequence, String from) {
        Set<State> reached = current;
        for (CallVariant step : sequence)
            reached = machine.after(reached, step);
        String calls = sequence.stream().map(this::call).collect(Collectors.joining(", then "));
        return new Hint(Kind.CALL_SEQUENCE, String.format("Call %s before this call to move %s from %s to %s", calls,
                receiver, from, names(reached)));
    }

    private Hint unreachable(Set<State> current, RefinedFunction call, boolean pathDependent) {
        if (pathDependent)
            return new Hint(Kind.PATH_DEPENDENT_STATE, String.format(
                    "%s can be in %s here, depending on the path taken, and no sequence of calls moves all of them to %s. "
                            + "Make %s reach this line in the same state on every path",
                    receiver, namesHere(current), needed(), receiver));
        String message = String.format("No sequence of calls moves %s from %s to %s", receiver, namesHere(current),
                needed());
        message += newObjects(call).map(c -> ". Create a new object instead with " + c).orElse("");
        return new Hint(Kind.UNREACHABLE_STATE, message);
    }

    private List<Hint> allowedCalls(Set<State> current) {
        List<RefinedFunction> allowed = machine.allowedIn(current);
        if (!allowed.isEmpty()) {
            List<String> signatures = allowed.stream().map(StateHints::signature).toList();
            String others = machine.complete() ? "" : " (and possibly others)";
            return List.of(new Hint(Kind.AVAILABLE_CALLS, String.format("In state %s, %s can call: %s%s",
                    namesHere(current), receiver, list(signatures), others)));
        }
        boolean noCallsInAnyState = current.stream().allMatch(s -> machine.allowedIn(Set.of(s)).isEmpty());
        if (machine.complete() && noCallsInAnyState)
            return List.of(new Hint(Kind.FINAL_STATE,
                    String.format("%s is in state %s, where none of its methods with state refinements can be called",
                            receiver, namesHere(current))));
        return List.of();
    }

    /** The constructors that create an object where {@code call} is allowed, unless the receiver is this object */
    private Optional<String> newObjects(RefinedFunction call) {
        if (receiver.equals(Keys.THIS))
            return Optional.empty();
        List<String> constructors = machine.constructorsEnabling(call).stream().map(StateHints::constructor).distinct()
                .toList();
        return constructors.isEmpty() ? Optional.empty() : Optional.of(String.join(" or ", constructors));
    }

    private String call(RefinedFunction method) {
        return receiver + "." + signature(method);
    }

    /**
     * A call as written in code, with {@code ...} for arguments the caller must supply, e.g. {@code l.tryLock(...)}:
     * the parameters' names are not in scope where the call is made
     */
    private String guardCall(RefinedFunction method) {
        String arguments = method.getArguments().isEmpty() ? "()" : "(...)";
        return receiver + "." + Utils.getSimpleName(method.getName()) + arguments;
    }

    /**
     * A method with its parameters' types and names, e.g. {@code tryLock(long timeout)}, for hints that mention the
     * parameters by name
     */
    private static String declaration(RefinedFunction method) {
        return Utils.getSimpleName(method.getName()) + namedParameters(method);
    }

    /**
     * A step of a sequence, with the condition on its arguments if it has one, e.g.
     * {@code p.setMode(int mode) with mode == 2}
     */
    private String call(CallVariant step) {
        if (step.kind() != CallVariant.Kind.ARGUMENTS)
            return call(step.method());
        return receiver + "." + declaration(step.method()) + " with "
                + step.condition().getExpression().toDisplayString();
    }

    /** A method as shown in hints, e.g. {@code open(int)} */
    public static String signature(RefinedFunction method) {
        return Utils.getSimpleName(method.getName()) + parameters(method);
    }

    private static String constructor(RefinedFunction constructor) {
        return "new " + Utils.getSimpleName(constructor.getTargetClass()) + parameters(constructor);
    }

    private static String namedParameters(RefinedFunction function) {
        return function.getArguments().stream().map(v -> v.getType().getSimpleName() + " " + v.getName())
                .collect(Collectors.joining(", ", "(", ")"));
    }

    private static String parameters(RefinedFunction function) {
        return function.getArguments().stream().map(Variable::getType).map(CtTypeReference::getSimpleName)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    /** {@code states} named as briefly as possible, e.g. {@code open or loud} */
    private String names(Collection<State> states) {
        return join(machine.describe(states));
    }

    /** The states the receiver can be in, named in the state sets the call checks and any set they all agree on */
    private String namesHere(Collection<State> current) {
        return join(machine.describe(current, checkedSets));
    }

    /**
     * The states that allow the call. If every state does, each by a different state refinement, the states each
     * refinement allows are named on their own: {@code a or b}, not every state.
     */
    private String needed() {
        Set<State> goal = machine.ordered(goals.stream().flatMap(Set::stream).toList());
        if (goal.size() < machine.states().size())
            return names(goal);
        return goals.stream().map(this::names).distinct().collect(Collectors.joining(" or "));
    }

    private static String join(List<List<String>> groups) {
        return groups.stream().map(group -> String.join(" and ", group)).collect(Collectors.joining(" or "));
    }

    private static String list(List<String> items) {
        if (items.size() <= MAX_LISTED_CALLS)
            return String.join(", ", items);
        return String.join(", ", items.subList(0, MAX_LISTED_CALLS)) + ", ...";
    }
}
