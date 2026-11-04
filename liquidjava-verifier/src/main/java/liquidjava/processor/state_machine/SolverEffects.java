package liquidjava.processor.state_machine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import liquidjava.diagnostics.DebugLog;
import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.ObjectState;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.constants.Keys;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;

/**
 * The {@link CallVariant.Effects} of a call, asked of the SMT solver only when a hint needs them, and then remembered.
 * The solver sees the object before and after the call, the call's parameters and its result under names nothing else
 * uses, so what the program knows about variables with the same names does not matter.
 */
final class SolverEffects implements CallVariant.Effects {

    static final String BEFORE = "#state_before";
    static final String AFTER = "#state_after";

    /**
     * The variables the predicates mention, by name, with their types
     *
     * @param states
     *            the states of the class
     * @param stateSets
     *            the states of each state set on its own, in the order the sets are declared
     * @param type
     *            the class of the object
     * @param free
     *            the parameters and the result, by their names in the predicates
     * @param element
     *            where the variables are declared
     */
    record Scope(TypeChecker tc, StateSolver solver, List<State> states, List<List<State>> stateSets,
            CtTypeReference<?> type, Map<String, CtTypeReference<?>> free, CtElement element) {
    }

    private final Scope scope;
    private final List<ObjectState> changes;
    /** The variant's condition, also assumed before the call unless it is on the result */
    private final Predicate condition;
    private final boolean onResult;
    /** The return refinement of a method without state refinements, or null */
    private final Predicate query;
    /** The effects with any arguments, whose entries a condition on the result does not change, or null */
    private final CallVariant.Effects any;

    /** By state refinement, then by the states before the call in the state sets its {@code from} names */
    private final Map<Integer, Map<List<String>, CallVariant.Entry>> entries = new HashMap<>();
    private final Map<Integer, Map<State, Optional<Set<State>>>> afters = new HashMap<>();
    /**
     * By state refinement, then by the states before the call in the sets its {@code to} names for {@code old(this)}:
     * the states it can lead to in the sets it names
     */
    private final Map<Integer, Map<List<String>, Set<List<String>>>> reachedBefore = new HashMap<>();

    /**
     * @param changes
     *            the state refinements that have a {@code from}, in the names of {@code scope}
     */
    SolverEffects(Scope scope, List<ObjectState> changes, Predicate condition, boolean onResult,
            CallVariant.Effects any) {
        this(scope, changes, condition, onResult, null, any);
    }

    /** The effects of checking the result of a method without state refinements, given its return refinement */
    static SolverEffects ofQuery(Scope scope, Predicate returnRefinement, Predicate condition) {
        return new SolverEffects(scope, List.of(), condition, true, returnRefinement, null);
    }

    private SolverEffects(Scope scope, List<ObjectState> changes, Predicate condition, boolean onResult,
            Predicate query, CallVariant.Effects any) {
        this.scope = scope;
        this.changes = changes;
        this.condition = condition;
        this.onResult = onResult;
        this.query = query;
        this.any = any;
    }

    @Override
    public int refinements() {
        return query != null ? 1 : changes.size();
    }

    @Override
    public boolean changesState(int index) {
        return query == null && changes.get(index).hasTo();
    }

    @Override
    public CallVariant.Entry entry(int index, State state) {
        if (query != null)
            return CallVariant.Entry.HOLDS; // no state refinement: allowed in every state
        if (onResult && any != null)
            return any.entry(index, state); // the result is not known before the call
        // the states of the other sets are about other ghost functions, so they cannot change the answer
        Predicate from = changes.get(index).getFrom();
        List<String> named = namedSets(from).stream().map(state.names()::get).toList();
        return entries.computeIfAbsent(index, i -> new HashMap<>()).computeIfAbsent(named, n -> {
            Optional<CallVariant.Entry> entry = withScope(() -> {
                Predicate before = before(state);
                Predicate fromBefore = from.substituteVariable(Keys.THIS, BEFORE);
                if (scope.solver().implies(before, fromBefore))
                    return CallVariant.Entry.HOLDS;
                return scope.solver().compatible(before, fromBefore) ? CallVariant.Entry.UNDECIDED
                        : CallVariant.Entry.FAILS;
            });
            return entry.orElse(CallVariant.Entry.UNDECIDED);
        });
    }

    @Override
    public Set<State> after(int index, State state) {
        Map<State, Optional<Set<State>>> known = afters.computeIfAbsent(index, i -> new HashMap<>());
        if (!known.containsKey(state)) {
            // a condition on the result only narrows the states the call can lead to
            Set<State> candidates = onResult && any != null ? any.after(index, state) : Set.copyOf(scope.states());
            known.put(state, candidates == null ? Optional.empty() : computeAfter(index, state, candidates));
        }
        return known.get(state).orElse(null);
    }

    private Optional<Set<State>> computeAfter(int index, State state, Set<State> candidates) {
        return withScope(() -> {
            if (query != null) {
                Predicate known = Predicate.createConjunction(before(state),
                        Predicate.createConjunction(query.substituteVariable(Keys.THIS, BEFORE), condition));
                return possibleStates(Set.of(state), known, BEFORE);
            }
            ObjectState change = changes.get(index);
            if (!change.hasTo())
                return Set.of(state);
            List<Integer> named = namedSets(change.getTo()).stream().sorted().toList();
            Set<State> kept = Set.copyOf(StateMachine.keeping(candidates, state, Set.copyOf(named)));
            // where it leads in the sets it names depends only on the state before in those it names for old(this)
            List<String> before = StateMachine.namedSetsBefore(change.getTo(), scope.stateSets()).stream().sorted()
                    .map(state.names()::get).toList();
            Map<List<String>, Set<List<String>>> reached = reachedBefore.computeIfAbsent(index, i -> new HashMap<>());
            if (!reached.containsKey(before)) {
                Predicate to = change.getTo().substituteVariable(Keys.THIS, AFTER).changeOldMentions(BEFORE, AFTER);
                Predicate known = Predicate.createConjunction(before(state), to);
                if (onResult)
                    known = Predicate.createConjunction(known, condition);
                reached.put(before, possibleStates(kept, known, AFTER).stream()
                        .map(c -> named.stream().map(c.names()::get).toList()).collect(Collectors.toSet()));
            }
            Set<State> next = scope.states().stream().filter(kept::contains)
                    .filter(c -> reached.get(before).contains(named.stream().map(c.names()::get).toList()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            // no state at all: the condition contradicts the refinement here, so nothing can be claimed
            return next.isEmpty() && !onResult ? null : next;
        });
    }

    /**
     * The {@code candidates} that {@code variable} can be in when {@code known} holds. Many candidates are first
     * narrowed down by the states possible in each state set on its own, which takes fewer questions than asking about
     * every combination.
     */
    private Set<State> possibleStates(Set<State> candidates, Predicate known, String variable) throws LJError {
        List<List<State>> sets = scope.stateSets();
        int setStates = sets.stream().mapToInt(List::size).sum();
        List<State> ordered = scope.states().stream().filter(candidates::contains).toList();
        if (ordered.size() <= setStates)
            return scope.solver().possibleStates(ordered, known, variable);
        List<Set<String>> possible = new ArrayList<>();
        for (int k = 0; k < sets.size(); k++) {
            int set = k;
            List<State> values = sets.get(k).stream()
                    .filter(v -> ordered.stream().anyMatch(c -> c.names().get(set).equals(v.names().get(0)))).toList();
            possible.add(scope.solver().possibleStates(values, known, variable).stream().map(v -> v.names().get(0))
                    .collect(Collectors.toSet()));
        }
        List<State> combined = ordered.stream().filter(c -> {
            for (int k = 0; k < sets.size(); k++)
                if (!possible.get(k).contains(c.names().get(k)))
                    return false;
            return true;
        }).toList();
        return scope.solver().possibleStates(combined, known, variable);
    }

    /** The state sets that {@code predicate} or this variant's condition may name a state of */
    private Set<Integer> namedSets(Predicate predicate) {
        Set<Integer> named = new HashSet<>(StateMachine.namedSets(predicate, scope.stateSets()));
        named.addAll(StateMachine.namedSets(condition, scope.stateSets()));
        return named;
    }

    /** What is known before the call in {@code state} */
    private Predicate before(State state) {
        Predicate before = state.predicate().substituteVariable(Keys.THIS, BEFORE);
        return onResult ? before : Predicate.createConjunction(before, condition);
    }

    private interface SolverQuery<T> {
        T ask() throws LJError;
    }

    /** Asks the solver with the variables of {@link #scope} declared; nothing if it fails */
    private <T> Optional<T> withScope(SolverQuery<T> query) {
        Context context = scope.tc().getContext();
        context.enterContext();
        try {
            context.addVarToContext(BEFORE, scope.type(), new Predicate(), scope.element());
            context.addVarToContext(AFTER, scope.type(), new Predicate(), scope.element());
            scope.free().forEach((name, type) -> context.addVarToContext(name, type, new Predicate(), scope.element()));
            return Optional.ofNullable(query.ask());
        } catch (RuntimeException e) { // including an LJError from the solver
            DebugLog.warn("Could not compute the effect of a call: " + e.getMessage());
            return Optional.empty();
        } finally {
            context.exitContext();
        }
    }
}
