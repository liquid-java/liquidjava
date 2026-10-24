package liquidjava.processor.state_machine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostFunction;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.ObjectState;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Builds the {@link StateMachine} of a class from its {@code @StateSet}s and {@code @StateRefinement}s. Every
 * transition is proven by the SMT solver, so the machine never contains a call that would not verify. Methods whose
 * state refinements mention anything besides the states of {@code this} (parameters, return values, other ghosts) are
 * left out, and the machine is then marked incomplete, as it is when a superclass or subclass has state refinements of
 * its own.
 */
public class StateMachineBuilder {

    /** Classes with more states than this get no machine, as building it would take too many solver calls */
    private static final int MAX_STATES = 16;

    // two objects of the class, before and after a call, that the rest of the program knows nothing about
    private static final String BEFORE = "#state_before";
    private static final String AFTER = "#state_after";

    private final TypeChecker tc;
    private final StateSolver solver;
    private final List<State> states;

    private StateMachineBuilder(TypeChecker tc, StateSolver solver, List<State> states) {
        this.tc = tc;
        this.solver = solver;
        this.states = states;
    }

    /** Whether the transitions of {@code function} can be part of a machine: they only depend on the state of this */
    public static boolean canModel(RefinedFunction function) {
        return function.getAllStates().stream().flatMap(change -> Stream.of(change.getFrom(), change.getTo()))
                .flatMap(p -> p.getVariableNames().stream()).allMatch(Keys.THIS::equals);
    }

    /**
     * Builds the state machine of {@code className}
     *
     * @param element
     *            the element being verified, where the fresh objects are declared
     *
     * @return the machine, or nothing if the class has no state sets or too many states
     */
    public static Optional<StateMachine> build(TypeChecker tc, StateSolver solver, String className, CtElement element)
            throws LJError {
        List<State> states = statesOf(tc.getContext(), className);
        if (states.isEmpty() || states.size() > MAX_STATES)
            return Optional.empty();
        Context context = tc.getContext();
        context.enterContext();
        try {
            CtTypeReference<?> type = tc.getFactory().Type().createReference(className);
            context.addVarToContext(BEFORE, type, new Predicate(), element);
            context.addVarToContext(AFTER, type, new Predicate(), element);
            return Optional.of(new StateMachineBuilder(tc, solver, states).build(className, type));
        } finally {
            context.exitContext();
        }
    }

    /** Every combination of one state from each state set of the class */
    private static List<State> statesOf(Context context, String className) {
        List<GhostState> ghostStates = context.getGhostState(Utils.getSimpleName(className));
        if (ghostStates == null)
            return List.of();
        Map<GhostFunction, List<GhostState>> stateSets = ghostStates.stream().filter(g -> g.getParent() != null)
                .collect(Collectors.groupingBy(GhostState::getParent, LinkedHashMap::new, Collectors.toList()));
        if (stateSets.isEmpty())
            return List.of();

        List<State> combinations = List.of(new State(List.of(), new Predicate()));
        for (List<GhostState> stateSet : stateSets.values())
            combinations = combinations.stream().flatMap(s -> stateSet.stream().map(g -> combine(s, g))).toList();
        return combinations;
    }

    private static State combine(State state, GhostState ghostState) {
        List<String> names = new ArrayList<>(state.names());
        names.add(ghostState.getName());
        Predicate holds = Predicate.createInvocation(ghostState.getQualifiedName(), Predicate.createVar(Keys.THIS));
        return new State(names, Predicate.createConjunction(state.predicate(), holds));
    }

    private StateMachine build(String className, CtTypeReference<?> type) throws LJError {
        List<RefinedFunction> methods = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();
        Map<RefinedFunction, Set<State>> constructors = new LinkedHashMap<>();
        boolean complete = !relatedClassChangesState(className, type);
        for (RefinedFunction function : tc.getContext().getCtxFunctions()) {
            if (!className.equals(function.getTargetClass()) || !function.hasStateChange())
                continue;
            if (CtExecutableReference.CONSTRUCTOR_NAME.equals(function.getName())) {
                Optional<Set<State>> created = createdStates(function);
                created.ifPresent(s -> constructors.put(function, s));
                continue;
            }
            Optional<List<Transition>> calls = transitionsOf(function);
            if (calls.isPresent()) {
                methods.add(function);
                transitions.addAll(calls.get());
            } else {
                complete = false;
            }
        }
        return new StateMachine(states, methods, transitions, constructors, complete);
    }

    /** Whether a superclass or subclass has state refinements of its own, which the machine does not include */
    private boolean relatedClassChangesState(String className, CtTypeReference<?> type) {
        return tc.getContext().getCtxFunctions().stream().filter(RefinedFunction::hasStateChange)
                .map(RefinedFunction::getTargetClass).filter(c -> c != null && !c.equals(className)).distinct()
                .map(c -> tc.getFactory().Type().createReference(c))
                .anyMatch(other -> other.isSubtypeOf(type) || type.isSubtypeOf(other));
    }

    /** The transitions of {@code method}, or nothing if they depend on more than the state of {@code this} */
    private Optional<List<Transition>> transitionsOf(RefinedFunction method) throws LJError {
        if (!canModel(method))
            return Optional.empty();
        List<Transition> transitions = new ArrayList<>();
        for (State state : states) {
            Predicate current = state.predicate().substituteVariable(Keys.THIS, BEFORE);
            for (ObjectState change : method.getAllStates()) {
                if (!change.hasFrom())
                    continue;
                Predicate from = change.getFrom().substituteVariable(Keys.THIS, BEFORE);
                if (solver.implies(current, from)) {
                    // like the verifier, the first state change whose precondition holds is the one applied
                    Set<State> next = statesAfter(change, state, current);
                    if (next.isEmpty())
                        return Optional.empty();
                    transitions.add(new Transition(state, method, change, next));
                    break;
                }
                if (solver.compatible(current, from))
                    return Optional.empty(); // allowed only sometimes in this state
            }
        }
        return Optional.of(transitions);
    }

    private Set<State> statesAfter(ObjectState change, State state, Predicate current) throws LJError {
        if (!change.hasTo())
            return Set.of(state);
        Predicate to = change.getTo().substituteVariable(Keys.THIS, AFTER).changeOldMentions(BEFORE, AFTER);
        return solver.possibleStates(states, Predicate.createConjunction(current, to), AFTER);
    }

    /** The states {@code constructor} can create the object in, or nothing if they depend on its arguments */
    private Optional<Set<State>> createdStates(RefinedFunction constructor) throws LJError {
        List<Predicate> toStates = constructor.getToStates();
        if (!canModel(constructor) || toStates.isEmpty())
            return Optional.empty();
        Predicate to = toStates.get(0).substituteVariable(Keys.THIS, AFTER);
        Set<State> created = solver.possibleStates(states, to, AFTER);
        return created.isEmpty() ? Optional.empty() : Optional.of(created);
    }
}
