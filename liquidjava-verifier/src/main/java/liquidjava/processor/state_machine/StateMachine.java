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

import liquidjava.processor.context.ObjectState;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.rj_language.Predicate;

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
 * @param variants
 *            the ways to call the methods whose state refinements also depend on their arguments or result, and the
 *            boolean methods whose result tells which state the object is in
 */
public record StateMachine(List<State> states, List<RefinedFunction> methods, List<Transition> transitions,
        Map<RefinedFunction, Set<State>> constructors, boolean complete, List<CallVariant> variants) {

    /** How many sets of states a search visits before giving up */
    private static final int MAX_SEARCHED = 1000;

    public StateMachine {
        states = List.copyOf(states);
        methods = List.copyOf(methods);
        transitions = List.copyOf(transitions);
        constructors = Collections.unmodifiableMap(new LinkedHashMap<>(constructors));
        variants = List.copyOf(variants);
    }

    public StateMachine(List<State> states, List<RefinedFunction> methods, List<Transition> transitions,
            Map<RefinedFunction, Set<State>> constructors, boolean complete) {
        this(states, methods, transitions, constructors, complete, List.of());
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

    /**
     * Whether {@code variant} can be called when the object can be in any of {@code current}. Like in the verifier, the
     * state refinement applied is the first whose {@code from} holds in all of them, and a method that keeps the state
     * needs each of them to satisfy some {@code from}. If the machine cannot tell, the call is not allowed.
     */
    public boolean allows(Set<State> current, CallVariant variant) {
        if (variant.effects() == null)
            return allows(current, variant.method());
        return applied(current, variant.effects()).isPresent();
    }

    /**
     * The states the object can be in after calling {@code variant}, which must be allowed in all of {@code current}
     */
    public Set<State> after(Set<State> current, CallVariant variant) {
        if (variant.effects() == null)
            return after(current, variant.method());
        CallVariant.Effects effects = variant.effects();
        List<Integer> applied = applied(current, effects).orElseThrow();
        return ordered(current.stream().flatMap(s -> applied.stream()
                .filter(i -> effects.entry(i, s) == CallVariant.Entry.HOLDS).flatMap(i -> effects.after(i, s).stream()))
                .toList());
    }

    /** The calls whose result, checked right after them, tells which state the object is in */
    public List<CallVariant> guards() {
        return variants.stream().filter(v -> v.kind() == CallVariant.Kind.RESULT).toList();
    }

    /** The indexes of the state refinements applied to a call made in any of {@code current}, if it is allowed */
    private Optional<List<Integer>> applied(Set<State> current, CallVariant.Effects effects) {
        if (current.isEmpty())
            return Optional.empty();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < effects.refinements(); i++)
            indexes.add(i);
        List<Integer> applied;
        if (indexes.stream().noneMatch(effects::changesState)) {
            boolean everyState = current.stream()
                    .allMatch(s -> indexes.stream().anyMatch(i -> effects.entry(i, s) == CallVariant.Entry.HOLDS));
            if (!everyState)
                return Optional.empty();
            applied = indexes;
        } else {
            Optional<Integer> first = firstApplying(current, effects);
            if (first.isEmpty())
                return Optional.empty();
            applied = List.of(first.get());
        }
        // the states after the call must be known too
        for (State s : current)
            for (int i : applied)
                if (effects.entry(i, s) == CallVariant.Entry.HOLDS && effects.after(i, s) == null)
                    return Optional.empty();
        return Optional.of(applied);
    }

    private static Optional<Integer> firstApplying(Set<State> current, CallVariant.Effects effects) {
        for (int i = 0; i < effects.refinements(); i++) {
            int index = i;
            List<CallVariant.Entry> entries = current.stream().map(s -> effects.entry(index, s)).toList();
            if (entries.contains(CallVariant.Entry.UNDECIDED))
                return Optional.empty(); // whether the verifier applies it depends on more than the state
            if (entries.stream().allMatch(CallVariant.Entry.HOLDS::equals))
                return Optional.of(i);
        }
        return Optional.empty();
    }

    /**
     * The calls a sequence can be made of when the object can be in any of {@code current}: the methods in the machine,
     * and the others with any arguments or with arguments meeting a condition that changes where they lead
     */
    private List<CallVariant> steps(Set<State> current) {
        List<CallVariant> steps = new ArrayList<>();
        for (RefinedFunction method : methods)
            if (allows(current, method))
                steps.add(new CallVariant(method, CallVariant.Kind.ANY, new Predicate(), null));
        for (CallVariant variant : variants) {
            if (variant.kind() == CallVariant.Kind.RESULT || !allows(current, variant))
                continue;
            Set<State> reached = after(current, variant);
            boolean sameAsAnyArguments = variant.kind() == CallVariant.Kind.ARGUMENTS && variants.stream()
                    .anyMatch(v -> v.method().equals(variant.method()) && v.kind() == CallVariant.Kind.ANY
                            && allows(current, v) && after(current, v).equals(reached));
            if (!reached.isEmpty() && !sameAsAnyArguments)
                steps.add(variant);
        }
        return steps;
    }

    /** The methods that can be called when the object can be in any of {@code current} */
    public List<RefinedFunction> allowedIn(Set<State> current) {
        List<RefinedFunction> allowed = new ArrayList<>(methods.stream().filter(m -> allows(current, m)).toList());
        variants.stream().filter(v -> v.kind() == CallVariant.Kind.ANY && allows(current, v)).map(CallVariant::method)
                .filter(m -> !allowed.contains(m)).forEach(allowed::add);
        return allowed;
    }

    /** The states where {@code method} can be called */
    public Set<State> statesAllowing(RefinedFunction method) {
        return ordered(statesAllowingByRefinement(method).stream().flatMap(Set::stream).toList());
    }

    /** The states the object can be in after calling {@code method}, which must be allowed in all of {@code current} */
    public Set<State> after(Set<State> current, RefinedFunction method) {
        return ordered(current.stream().flatMap(s -> transition(s, method).orElseThrow().to().stream()).toList());
    }

    /**
     * The shortest sequences of calls that take an object in any of {@code current} to where {@code call} is allowed. A
     * call of a method whose state refinements depend on its arguments has the condition on them that the sequence
     * needs, if any; a sequence that needs none is preferred.
     *
     * @return at most {@code limit} sequences, all of the same length; none if there is no such sequence; nothing if
     *         the search gave up
     */
    public Optional<List<List<CallVariant>>> shortestPaths(Set<State> current, RefinedFunction call, int limit) {
        Map<Set<State>, List<List<CallVariant>>> level = Map.of(current, List.of(List.of()));
        Set<Set<State>> visited = new HashSet<>(level.keySet());
        while (!level.isEmpty()) {
            Map<Set<State>, List<List<CallVariant>>> next = new LinkedHashMap<>();
            level.forEach((states, paths) -> {
                for (CallVariant step : steps(states)) {
                    Set<State> reached = after(states, step);
                    if (visited.contains(reached))
                        continue;
                    List<List<CallVariant>> reachedPaths = next.computeIfAbsent(reached, k -> new ArrayList<>());
                    paths.stream().limit(limit - reachedPaths.size()).map(path -> append(path, step))
                            .forEach(reachedPaths::add);
                }
            });
            List<List<CallVariant>> found = next.entrySet().stream().filter(e -> allows(e.getKey(), call))
                    .flatMap(e -> e.getValue().stream()).toList();
            if (!found.isEmpty()) {
                List<List<CallVariant>> unconditioned = found.stream()
                        .filter(p -> p.stream().noneMatch(v -> v.kind() == CallVariant.Kind.ARGUMENTS)).toList();
                return Optional.of((unconditioned.isEmpty() ? found : unconditioned).stream().limit(limit).toList());
            }
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
     * {@code subset} named as briefly as possible, as groups of states that each fix the state in some state sets and
     * allow any state in the others, e.g. {@code [[open], [loud]]} for the states where {@code open(this) ||
     * loud(this)} holds
     */
    public List<List<String>> describe(Collection<State> subset) {
        return describe(subset, Set.of());
    }

    /** Like {@link #describe(Collection)}, but naming the state of every set in {@code named} */
    public List<List<String>> describe(Collection<State> subset, Set<Integer> named) {
        return StateCover.of(states, Set.copyOf(subset), named);
    }

    /**
     * The state sets that {@code subset} depends on, as indexes into {@link State#names()}: those its
     * {@link #describe(Collection) description} names
     */
    public Set<Integer> stateSetsNamed(Collection<State> subset) {
        return StateCover.namedSets(states, Set.copyOf(subset));
    }

    /** The states where {@code method} can be called, for each of its state refinements that applies in some state */
    public List<Set<State>> statesAllowingByRefinement(RefinedFunction method) {
        Map<ObjectState, List<State>> byChange = new LinkedHashMap<>();
        transitions.stream().filter(t -> t.method().equals(method))
                .forEach(t -> byChange.computeIfAbsent(t.change(), c -> new ArrayList<>()).add(t.from()));
        return byChange.values().stream().map(this::ordered).toList();
    }

    /** {@code subset} in declaration order */
    public Set<State> ordered(Collection<State> subset) {
        return states.stream().filter(subset::contains).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Optional<Transition> transition(State from, RefinedFunction method) {
        return transitions.stream().filter(t -> t.from().equals(from) && t.method().equals(method)).findFirst();
    }

    private static List<CallVariant> append(List<CallVariant> path, CallVariant method) {
        List<CallVariant> extended = new ArrayList<>(path);
        extended.add(method);
        return extended;
    }
}
