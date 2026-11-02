package liquidjava.processor.state_machine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import liquidjava.diagnostics.errors.LJError;
import liquidjava.processor.context.Context;
import liquidjava.processor.context.GhostState;
import liquidjava.processor.context.ObjectState;
import liquidjava.processor.context.RefinedFunction;
import liquidjava.processor.refinement_checker.TypeChecker;
import liquidjava.rj_language.Predicate;
import liquidjava.utils.Utils;
import liquidjava.utils.constants.Keys;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtModifiable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Builds the {@link StateMachine} of a class from its {@code @StateSet}s and {@code @StateRefinement}s, and those of
 * its supertypes, which it inherits. Every transition is proven by the SMT solver, so the machine never contains a call
 * that would not verify. Methods whose state refinements mention anything besides the states of {@code this}
 * (parameters, return values, other ghosts) are left out, and the machine is then marked incomplete, as it is when a
 * subclass has state refinements of its own or an inherited refinement may not be the one the verifier applies.
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
        CtTypeReference<?> type = tc.getFactory().Type().createReference(className);
        List<CtTypeReference<?>> supertypes = supertypesOf(type);
        List<State> states = statesOf(ghostStatesOf(tc.getContext(), type, supertypes), false);
        if (states.isEmpty() || states.size() > MAX_STATES)
            return Optional.empty();
        Context context = tc.getContext();
        context.enterContext();
        try {
            context.addVarToContext(BEFORE, type, new Predicate(), element);
            context.addVarToContext(AFTER, type, new Predicate(), element);
            return Optional.of(new StateMachineBuilder(tc, solver, states).build(type, supertypes));
        } finally {
            context.exitContext();
        }
    }

    /** The ghost states of {@code className} and of its supertypes, which its objects also have */
    public static List<GhostState> ghostStatesOf(TypeChecker tc, String className) {
        CtTypeReference<?> type = tc.getFactory().Type().createReference(className);
        return ghostStatesOf(tc.getContext(), type, supertypesOf(type));
    }

    private static List<GhostState> ghostStatesOf(Context context, CtTypeReference<?> type,
            List<CtTypeReference<?>> supertypes) {
        // a state set can be registered more than once, so states are told apart by name, and states are kept by
        // simple class name, so those of another class with the same simple name are left out
        Map<String, GhostState> ghostStates = new LinkedHashMap<>();
        for (CtTypeReference<?> t : Stream.concat(Stream.of(type), supertypes.stream()).toList()) {
            List<GhostState> declared = context.getGhostState(Utils.getSimpleName(t.getQualifiedName()));
            if (declared != null)
                declared.stream().filter(g -> t.getQualifiedName().equals(g.getPrefix()))
                        .forEach(g -> ghostStates.putIfAbsent(g.getQualifiedName(), g));
        }
        return List.copyOf(ghostStates.values());
    }

    /** Every supertype of {@code type}, nearest first: its superclasses, then the interfaces they implement */
    private static List<CtTypeReference<?>> supertypesOf(CtTypeReference<?> type) {
        List<CtTypeReference<?>> superclasses = new ArrayList<>();
        Deque<CtTypeReference<?>> interfaces = new ArrayDeque<>();
        Set<String> seen = new HashSet<>(Set.of(type.getQualifiedName()));
        for (CtTypeReference<?> t = type; t != null; t = superclassOf(t)) {
            if (t != type && seen.add(t.getQualifiedName()))
                superclasses.add(t);
            interfaces.addAll(superInterfacesOf(t));
        }
        List<CtTypeReference<?>> supertypes = new ArrayList<>(superclasses);
        while (!interfaces.isEmpty()) {
            CtTypeReference<?> t = interfaces.poll();
            if (seen.add(t.getQualifiedName())) {
                supertypes.add(t);
                interfaces.addAll(superInterfacesOf(t));
            }
        }
        return supertypes;
    }

    // a supertype that cannot be resolved (no source, not on the classpath) ends the walk on that branch
    private static CtTypeReference<?> superclassOf(CtTypeReference<?> type) {
        try {
            return type.getSuperclass();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Set<CtTypeReference<?>> superInterfacesOf(CtTypeReference<?> type) {
        try {
            return type.getSuperInterfaces();
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    /**
     * Every combination of one state from each state set of the class
     *
     * @param withNone
     *            whether an object may also be in none of the states of a set, named {@code none} (e.g. after
     *            {@code @StateRefinement(to = "!open(this) && !closed(this)")})
     */
    public static List<State> statesOf(Context context, String className, boolean withNone) {
        List<GhostState> ghostStates = context.getGhostState(Utils.getSimpleName(className));
        return ghostStates == null ? List.of() : statesOf(ghostStates, withNone);
    }

    private static List<State> statesOf(List<GhostState> ghostStates, boolean withNone) {
        Map<String, List<GhostState>> stateSets = ghostStates.stream().filter(g -> g.getParent() != null).collect(
                Collectors.groupingBy(g -> g.getParent().getQualifiedName(), LinkedHashMap::new, Collectors.toList()));
        if (stateSets.isEmpty())
            return List.of();

        List<State> combinations = List.of(new State(List.of(), new Predicate()));
        for (List<GhostState> stateSet : stateSets.values()) {
            List<State> inSet = Stream.concat(stateSet.stream().map(StateMachineBuilder::inState),
                    withNone ? Stream.of(inNone(stateSet)) : Stream.empty()).toList();
            combinations = combinations.stream().flatMap(s -> inSet.stream().map(a -> combine(s, a))).toList();
        }
        return combinations;
    }

    private static State inState(GhostState ghostState) {
        return new State(List.of(ghostState.getName()),
                Predicate.createInvocation(ghostState.getQualifiedName(), Predicate.createVar(Keys.THIS)));
    }

    private static State inNone(List<GhostState> stateSet) {
        Predicate none = stateSet.stream().map(g -> inState(g).predicate().negate())
                .reduce(Predicate::createConjunction).orElseThrow();
        return new State(List.of("none"), none);
    }

    private static State combine(State state, State inSet) {
        List<String> names = new ArrayList<>(state.names());
        names.addAll(inSet.names());
        return new State(names, Predicate.createConjunction(state.predicate(), inSet.predicate()));
    }

    private StateMachine build(CtTypeReference<?> type, List<CtTypeReference<?>> supertypes) throws LJError {
        String className = type.getQualifiedName();
        Set<String> inherited = supertypes.stream().map(CtTypeReference::getQualifiedName).collect(Collectors.toSet());
        List<RefinedFunction> methods = new ArrayList<>();
        List<Transition> transitions = new ArrayList<>();
        Map<RefinedFunction, Set<State>> constructors = new LinkedHashMap<>();
        boolean complete = !subclassChangesState(className, type, inherited);
        for (RefinedFunction function : tc.getContext().getCtxFunctions()) {
            if (!function.hasStateChange())
                continue;
            boolean constructor = CtExecutableReference.CONSTRUCTOR_NAME.equals(function.getName());
            if (className.equals(function.getTargetClass()) && constructor) {
                if (abstractType(type) || privateConstructor(function, type))
                    continue; // cannot be suggested as a new object
                Optional<Set<State>> created = createdStates(function);
                created.ifPresent(s -> constructors.put(function, s));
                continue;
            }
            if (className.equals(function.getTargetClass()) && !appliesToOwnType(function, type, supertypes)) {
                complete = false;
                continue;
            }
            if (!className.equals(function.getTargetClass())) {
                if (constructor || !inherited.contains(function.getTargetClass()))
                    continue;
                if (overridden(function, className))
                    continue; // the class's own refinement for it is the one in the machine
                if (!resolvesTo(function, type, supertypes)) {
                    complete = false;
                    continue;
                }
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

    /** Whether a subclass has state refinements of its own, which the machine does not include */
    private boolean subclassChangesState(String className, CtTypeReference<?> type, Set<String> inherited) {
        return tc.getContext().getCtxFunctions().stream().filter(RefinedFunction::hasStateChange)
                .map(RefinedFunction::getTargetClass)
                .filter(c -> c != null && !c.equals(className) && !inherited.contains(c)).distinct()
                .map(c -> tc.getFactory().Type().createReference(c)).anyMatch(other -> other.isSubtypeOf(type));
    }

    /**
     * Whether the verifier checks a call to {@code method}, specified for {@code type}, on an object of that type. It
     * looks the refinement up by the receiver's type, which needs the parameter types to match those of the method it
     * resolves to, and otherwise by the type that declares that method. A parameter whose type is a type variable may
     * not match, so then {@code type} must declare the method.
     */
    private boolean appliesToOwnType(RefinedFunction method, CtTypeReference<?> type,
            List<CtTypeReference<?>> supertypes) {
        boolean concreteParameters = method.getArguments().stream()
                .noneMatch(a -> a.getType() instanceof CtTypeParameterReference);
        return concreteParameters || resolvesTo(method, type, supertypes);
    }

    /** Whether {@code className} has its own refinement for {@code method}, with the same parameter types */
    private boolean overridden(RefinedFunction method, String className) {
        String name = Utils.getSimpleName(method.getName());
        List<String> parameters = parameterTypes(method);
        return tc.getContext().getCtxFunctions().stream().anyMatch(f -> className.equals(f.getTargetClass())
                && name.equals(Utils.getSimpleName(f.getName())) && parameters.equals(parameterTypes(f)));
    }

    private static List<String> parameterTypes(RefinedFunction function) {
        return function.getArguments().stream().map(a -> typeName(a.getType())).toList();
    }

    /**
     * Whether the nearest type in the hierarchy of {@code type} that declares {@code method} is the one it is specified
     * for, which is where the verifier looks up the refinement of a call. Methods are matched by name and number of
     * parameters, so an overload can hide an inherited method, which only leaves the machine incomplete.
     */
    private boolean resolvesTo(RefinedFunction method, CtTypeReference<?> type, List<CtTypeReference<?>> supertypes) {
        String name = Utils.getSimpleName(method.getName());
        int arity = method.getArguments().size();
        for (CtTypeReference<?> t : Stream.concat(Stream.of(type), supertypes.stream()).toList()) {
            CtType<?> declaration = t.getTypeDeclaration();
            if (declaration == null)
                return false; // cannot tell which type declares it
            boolean declares = declaration.getMethods().stream()
                    .anyMatch(m -> m.getSimpleName().equals(name) && m.getParameters().size() == arity);
            if (declares)
                return t.getQualifiedName().equals(method.getTargetClass());
        }
        return false;
    }

    private static boolean abstractType(CtTypeReference<?> type) {
        CtType<?> declaration = type.getTypeDeclaration();
        return declaration == null || declaration.isInterface() || declaration.isAbstract();
    }

    /**
     * Whether {@code constructor} cannot be called from outside its class: it is private, or the class is an enum. A
     * constructor that is not found in the class's declaration is assumed callable. The machine is shared by every
     * caller, so a private constructor is left out even for calls inside its class.
     */
    private static boolean privateConstructor(RefinedFunction constructor, CtTypeReference<?> type) {
        try {
            CtType<?> declaration = type.getTypeDeclaration();
            if (declaration.isEnum())
                return true;
            if (!(declaration instanceof CtClass<?> declared))
                return false;
            List<String> parameters = parameterTypes(constructor);
            return declared.getConstructors().stream()
                    .filter(c -> c.getParameters().stream().map(p -> typeName(p.getType())).toList().equals(parameters))
                    .anyMatch(CtModifiable::isPrivate);
        } catch (RuntimeException e) {
            return false; // a declaration that cannot be built is not evidence that the constructor is private
        }
    }

    private static String typeName(CtTypeReference<?> type) {
        return type == null ? "" : type.getQualifiedName();
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
