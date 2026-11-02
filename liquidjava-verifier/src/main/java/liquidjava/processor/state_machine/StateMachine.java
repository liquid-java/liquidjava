package liquidjava.processor.state_machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import liquidjava.processor.context.ObjectState;
import liquidjava.processor.context.RefinedFunction;

/**
 * The typestate of a class as a finite state machine, built by {@link StateMachineBuilder}
 *
 * @param states
 *            all states of the class, in declaration order
 * @param methods
 *            the methods whose transitions are all known, in declaration order
 * @param transitions
 *            the transitions of those methods
 * @param constructors
 *            the states each constructor can create the object in
 * @param complete
 *            whether every method that can change the state of the object is in {@code methods}; only then can the
 *            machine tell that a state cannot be reached or that no call is allowed
 */
public record StateMachine(List<State> states, List<RefinedFunction> methods, List<Transition> transitions,
        Map<RefinedFunction, Set<State>> constructors, boolean complete) {

    /** How many sets of states a search visits before giving up */
    private static final int MAX_SEARCHED = 1000;

    public StateMachine {
        states = List.copyOf(states);
        methods = List.copyOf(methods);
        transitions = List.copyOf(transitions);
        constructors = Collections.unmodifiableMap(new LinkedHashMap<>(constructors));
    }

    public boolean models(RefinedFunction method) {
        return methods.contains(method);
    }

    /**
     * Whether {@code method} can be called when the object can be in any of {@code current}. Like in the verifier, a
     * method that changes the state needs a single one of its state refinements to allow all of them.
     */
    public boolean allows(Set<State> current, RefinedFunction method) {
        List<Transition> calls = current.stream().map(s -> transition(s, method)).flatMap(Optional::stream).toList();
        if (calls.size() != current.size())
            return false;
        boolean preservesState = method.getAllStates().stream().noneMatch(ObjectState::hasTo);
        return preservesState || calls.stream().map(Transition::change).distinct().count() <= 1;
    }

    /** The methods that can be called when the object can be in any of {@code current} */
    public List<RefinedFunction> allowedIn(Set<State> current) {
        return methods.stream().filter(m -> allows(current, m)).toList();
    }

    /** The states where {@code method} can be called */
    public Set<State> statesAllowing(RefinedFunction method) {
        return ordered(transitions.stream().filter(t -> t.method().equals(method)).map(Transition::from).toList());
    }

    /** The states the object can be in after calling {@code method}, which must be allowed in all of {@code current} */
    public Set<State> after(Set<State> current, RefinedFunction method) {
        return ordered(current.stream().flatMap(s -> transition(s, method).orElseThrow().to().stream()).toList());
    }

    /**
     * The shortest sequences of calls that take an object in any of {@code current} to where {@code call} is allowed
     *
     * @return at most {@code limit} sequences, all of the same length; none if there is no such sequence; nothing if
     *         the search gave up
     */
    public Optional<List<List<RefinedFunction>>> shortestPaths(Set<State> current, RefinedFunction call, int limit) {
        Map<Set<State>, List<List<RefinedFunction>>> level = Map.of(current, List.of(List.of()));
        Set<Set<State>> visited = new HashSet<>(level.keySet());
        while (!level.isEmpty()) {
            Map<Set<State>, List<List<RefinedFunction>>> next = new LinkedHashMap<>();
            level.forEach((states, paths) -> {
                for (RefinedFunction method : allowedIn(states)) {
                    Set<State> reached = after(states, method);
                    if (visited.contains(reached))
                        continue;
                    List<List<RefinedFunction>> reachedPaths = next.computeIfAbsent(reached, k -> new ArrayList<>());
                    paths.stream().limit(limit - reachedPaths.size()).map(path -> append(path, method))
                            .forEach(reachedPaths::add);
                }
            });
            List<List<RefinedFunction>> found = next.entrySet().stream().filter(e -> allows(e.getKey(), call))
                    .flatMap(e -> e.getValue().stream()).limit(limit).toList();
            if (!found.isEmpty())
                return Optional.of(found);
            visited.addAll(next.keySet());
            if (visited.size() > MAX_SEARCHED)
                return Optional.empty();
            level = next;
        }
        return Optional.of(List.of());
    }

    /** The constructors that create the object in a state where {@code call} is allowed */
    public List<RefinedFunction> constructorsEnabling(RefinedFunction call) {
        return constructors.entrySet().stream().filter(e -> allows(e.getValue(), call)).map(Map.Entry::getKey).toList();
    }

    /**
     * The state sets that {@code subset} depends on, as indexes into {@link State#names()}: those where changing only
     * the state in that set can take a state out of {@code subset}. Every other state set can be in any of its states.
     */
    public List<Integer> stateSetsConstraining(Set<State> subset) {
        int stateSets = states.isEmpty() ? 0 : states.get(0).names().size();
        return IntStream.range(0, stateSets).filter(set -> subset.stream().anyMatch(
                state -> states.stream().anyMatch(other -> !subset.contains(other) && differOnlyIn(state, other, set))))
                .boxed().toList();
    }

    /** {@code subset} in declaration order */
    public Set<State> ordered(Collection<State> subset) {
        return states.stream().filter(subset::contains).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Optional<Transition> transition(State from, RefinedFunction method) {
        return transitions.stream().filter(t -> t.from().equals(from) && t.method().equals(method)).findFirst();
    }

    private static boolean differOnlyIn(State state, State other, int stateSet) {
        return IntStream.range(0, state.names().size())
                .allMatch(set -> set == stateSet || state.names().get(set).equals(other.names().get(set)));
    }

    private static List<RefinedFunction> append(List<RefinedFunction> path, RefinedFunction method) {
        List<RefinedFunction> extended = new ArrayList<>(path);
        extended.add(method);
        return extended;
    }
}
